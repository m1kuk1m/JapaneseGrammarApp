package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectorEngine
import com.example.japanesegrammarapp.domain.model.OcrBoxPreviewMode
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun detectCameraOcrBoxes(
    bitmap: Bitmap,
    settings: OcrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT,
    context: Context? = null
): List<Rect> {
    val normalizedSettings = settings.normalized()
    return when (normalizedSettings.detectorEngine) {
        OcrBoxDetectorEngine.ML_KIT -> detectMlKitCameraOcrBoxes(bitmap, normalizedSettings)
        OcrBoxDetectorEngine.RAPID_OCR -> detectRapidOcrCameraBoxes(
            context = context ?: error("RapidOCR detection requires Android context"),
            bitmap = bitmap,
            settings = normalizedSettings
        )
        OcrBoxDetectorEngine.HYBRID -> detectHybridCameraOcrBoxes(
            context = context ?: error("Hybrid OCR detection requires Android context"),
            bitmap = bitmap,
            settings = normalizedSettings
        )
        OcrBoxDetectorEngine.AUTO -> runCatching {
            detectHybridCameraOcrBoxes(
                context = context ?: error("RapidOCR detection requires Android context"),
                bitmap = bitmap,
                settings = normalizedSettings
            )
        }.getOrElse { error ->
            AppLogger.e("CAMERA", "Hybrid OCR box detection failed; falling back to ML Kit", error)
            detectMlKitCameraOcrBoxes(bitmap, normalizedSettings.copy(detectorEngine = OcrBoxDetectorEngine.ML_KIT))
        }
    }
}

private suspend fun detectHybridCameraOcrBoxes(
    context: Context,
    bitmap: Bitmap,
    settings: OcrBoxDetectionSettings
): List<Rect> {
    val normalized = settings.normalized()
    if (normalized.previewMode == OcrBoxPreviewMode.RAW) {
        return detectRapidOcrRawCameraBoxes(context, bitmap, normalized)
    }

    val mlKitBoxes = detectMlKitCameraOcrBoxes(
        bitmap = bitmap,
        settings = normalized.copy(
            detectorEngine = OcrBoxDetectorEngine.ML_KIT,
            previewMode = OcrBoxPreviewMode.FINAL
        )
    )
    val rapidRawBoxes = detectRapidOcrRawCameraBoxes(context, bitmap, normalized)

    return mergeHybridBoxes(
        mlKitBoxes = mlKitBoxes,
        rapidRawBoxes = rapidRawBoxes,
        bitmapWidth = bitmap.width,
        bitmapHeight = bitmap.height,
        settings = normalized
    )
}

private suspend fun detectMlKitCameraOcrBoxes(
    bitmap: Bitmap,
    settings: OcrBoxDetectionSettings
): List<Rect> = suspendCancellableCoroutine { continuation ->
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    val image = InputImage.fromBitmap(bitmap, 0)
    val normalizedSettings = settings.normalized()

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val candidates = buildBlockCandidates(visionText.textBlocks)
            if (continuation.isActive) {
                val boxes = if (normalizedSettings.previewMode == OcrBoxPreviewMode.RAW) {
                    candidates.map { it.rect }.sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
                } else {
                    mergeNearbyBlockCandidates(
                        candidates = candidates,
                        bitmapWidth = bitmap.width,
                        bitmapHeight = bitmap.height,
                        settings = normalizedSettings
                    )
                }
                continuation.resume(boxes)
            }
        }
        .addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
        .addOnCompleteListener {
            recognizer.close()
        }

    continuation.invokeOnCancellation {
        recognizer.close()
    }
}

private enum class OcrTextDirection {
    HORIZONTAL,
    VERTICAL,
    MIXED
}

private data class OcrBlockCandidate(
    val rect: Rect,
    val direction: OcrTextDirection,
    val averageLineThickness: Float
)

private data class OcrMergedCandidate(
    val rect: Rect,
    val direction: OcrTextDirection,
    val averageLineThickness: Float
)

private fun buildBlockCandidates(blocks: List<Text.TextBlock>): List<OcrBlockCandidate> {
    return blocks.flatMap { block -> buildCandidatesForBlock(block) }
}

private fun buildCandidatesForBlock(block: Text.TextBlock): List<OcrBlockCandidate> {
    val lineRects = block.lines.mapNotNull { line ->
        line.boundingBox?.takeIf { it.width() > 0 && it.height() > 0 }
    }
    val blockRect = block.boundingBox?.takeIf { it.width() > 0 && it.height() > 0 }
        ?: unionRects(lineRects)
        ?: return emptyList()
    val direction = detectTextDirection(lineRects, blockRect)

    if (lineRects.size <= 1 || direction == OcrTextDirection.MIXED || direction == OcrTextDirection.VERTICAL) {
        return listOf(
            OcrBlockCandidate(
                rect = blockRect,
                direction = direction,
                averageLineThickness = averageLineThickness(lineRects, blockRect, direction)
            )
        )
    }

    return splitLineRectsByRhythm(lineRects, direction).mapNotNull { group ->
        val rect = unionRects(group) ?: return@mapNotNull null
        OcrBlockCandidate(
            rect = rect,
            direction = direction,
            averageLineThickness = averageLineThickness(group, rect, direction)
        )
    }
}

private fun mergeNearbyBlockCandidates(
    candidates: List<OcrBlockCandidate>,
    bitmapWidth: Int,
    bitmapHeight: Int,
    settings: OcrBoxDetectionSettings
): List<Rect> {
    val baseCandidates = candidates
        .mapNotNull { candidate -> toMergedCandidate(listOf(candidate)) }
        .filter { candidate -> isPlausibleCandidate(candidate, bitmapWidth, bitmapHeight) }
    val nearbyMergedCandidates = mergeNearbyCandidates(baseCandidates, settings)
        .filter { candidate -> isPlausibleCandidate(candidate, bitmapWidth, bitmapHeight) }
    val overlapMergedCandidates = mergeStrongOverlappingCandidates(nearbyMergedCandidates)
        .filter { candidate -> isPlausibleCandidate(candidate, bitmapWidth, bitmapHeight) }
    val dedupedCandidates = dedupeOverlappingCandidates(overlapMergedCandidates)

    return dedupedCandidates.map { candidate ->
        paddedRect(candidate, bitmapWidth, bitmapHeight, settings)
    }.sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
}

private fun toMergedCandidate(cluster: List<OcrBlockCandidate>): OcrMergedCandidate? {
    val rect = unionRects(cluster.map { it.rect }) ?: return null
    val direction = cluster.map { it.direction }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: detectTextDirection(emptyList(), rect)
    val thicknesses = cluster.map { it.averageLineThickness }.sorted()
    val averageThickness = thicknesses[thicknesses.size / 2].coerceAtLeast(1f)

    return OcrMergedCandidate(
        rect = rect,
        direction = direction,
        averageLineThickness = averageThickness
    )
}

private fun splitLineRectsByRhythm(
    lineRects: List<Rect>,
    direction: OcrTextDirection
): List<List<Rect>> {
    if (lineRects.size <= 1 || direction == OcrTextDirection.MIXED) {
        return listOf(lineRects)
    }

    val sortedRects = when (direction) {
        OcrTextDirection.HORIZONTAL -> lineRects.sortedBy { it.centerY() }
        OcrTextDirection.VERTICAL -> lineRects.sortedBy { it.centerX() }
        OcrTextDirection.MIXED -> lineRects
    }
    val centerGaps = sortedRects.zipWithNext { current, next ->
        when (direction) {
            OcrTextDirection.HORIZONTAL -> kotlin.math.abs(next.centerY() - current.centerY()).toFloat()
            OcrTextDirection.VERTICAL -> kotlin.math.abs(next.centerX() - current.centerX()).toFloat()
            OcrTextDirection.MIXED -> 0f
        }
    }.filter { it > 0f }
    val baselineGap = rhythmBaselineGap(centerGaps)
    val medianThickness = medianOrNull(sortedRects.map { lineThickness(it, direction) }) ?: 1f
    val fallbackBreakGap = medianThickness * 2.2f

    val groups = mutableListOf<MutableList<Rect>>()
    var currentGroup = mutableListOf(sortedRects.first())

    for (index in 1 until sortedRects.size) {
        val previous = sortedRects[index - 1]
        val current = sortedRects[index]
        val gap = when (direction) {
            OcrTextDirection.HORIZONTAL -> kotlin.math.abs(current.centerY() - previous.centerY()).toFloat()
            OcrTextDirection.VERTICAL -> kotlin.math.abs(current.centerX() - previous.centerX()).toFloat()
            OcrTextDirection.MIXED -> 0f
        }

        val referenceGap = baselineGap ?: fallbackBreakGap
        val rhythmMismatch = gap > referenceGap * 1.18f && gap - referenceGap > medianThickness * 0.35f
        val absoluteBreak = gap > maxOf(referenceGap * 1.28f, fallbackBreakGap)

        if (rhythmMismatch || absoluteBreak) {
            groups.add(currentGroup)
            currentGroup = mutableListOf(current)
        } else {
            currentGroup.add(current)
        }
    }
    groups.add(currentGroup)

    return groups
}

private fun mergeNearbyCandidates(
    candidates: List<OcrMergedCandidate>,
    settings: OcrBoxDetectionSettings
): List<OcrMergedCandidate> {
    val merged = candidates.toMutableList()

    var changed = true
    while (changed) {
        changed = false
        for (i in merged.indices) {
            for (j in i + 1 until merged.size) {
                if (shouldMergeNearbyCandidates(merged[i], merged[j], settings)) {
                    merged[i] = mergeCandidates(
                        candidateI = merged[i],
                        candidateJ = merged[j],
                        direction = mergedDirection(merged[i], merged[j])
                    )
                    merged.removeAt(j)
                    changed = true
                    break
                }
            }
            if (changed) break
        }
    }

    return merged
}

private fun shouldMergeNearbyCandidates(
    candidateI: OcrMergedCandidate,
    candidateJ: OcrMergedCandidate,
    settings: OcrBoxDetectionSettings
): Boolean {
    return when (commonMergeDirection(candidateI, candidateJ)) {
        OcrTextDirection.VERTICAL -> shouldMergeVerticalNeighbors(candidateI, candidateJ, settings)
        OcrTextDirection.HORIZONTAL -> shouldMergeHorizontalNeighbors(candidateI, candidateJ, settings)
        else -> false
    }
}

private fun shouldMergeVerticalNeighbors(
    candidateI: OcrMergedCandidate,
    candidateJ: OcrMergedCandidate,
    settings: OcrBoxDetectionSettings
): Boolean {
    val rectI = candidateI.rect
    val rectJ = candidateJ.rect
    val averageThickness = medianOf(candidateI.averageLineThickness, candidateJ.averageLineThickness)
    val horizontalGap = gapX(rectI, rectJ)
    val verticalGap = gapY(rectI, rectJ)
    val yOverlap = overlapRatio(rectI.top, rectI.bottom, rectJ.top, rectJ.bottom)
    val xOverlap = overlapRatio(rectI.left, rectI.right, rectJ.left, rectJ.right)
    val union = unionRects(listOf(rectI, rectJ)) ?: return false
    val centerYDistance = kotlin.math.abs(rectI.centerY() - rectJ.centerY())
    val heightRatio = maxOf(rectI.height(), rectJ.height()).toFloat() /
        minOf(rectI.height(), rectJ.height()).coerceAtLeast(1).toFloat()
    val minWidth = minOf(rectI.width(), rectJ.width()).coerceAtLeast(1)
    val fillRatio = mergedTextFillRatio(rectI, rectJ, union)

    val closeColumns = horizontalGap <= maxOf(averageThickness * settings.verticalColumnGapMultiplier, minWidth * 0.55f, 24f) ||
        xOverlap >= settings.verticalXOverlapThreshold
    val sameVerticalBand = yOverlap >= 0.48f ||
        (centerYDistance <= union.height() * 0.28f && verticalGap <= averageThickness * 2.6f)
    val comparableHeight = heightRatio <= 3.0f
    val compactUnion = fillRatio >= settings.verticalFillRatioMin

    return closeColumns && sameVerticalBand && comparableHeight && compactUnion
}

private fun shouldMergeHorizontalNeighbors(
    candidateI: OcrMergedCandidate,
    candidateJ: OcrMergedCandidate,
    settings: OcrBoxDetectionSettings
): Boolean {
    val rectI = candidateI.rect
    val rectJ = candidateJ.rect
    val averageThickness = medianOf(candidateI.averageLineThickness, candidateJ.averageLineThickness)
    val verticalGap = gapY(rectI, rectJ)
    val horizontalGap = gapX(rectI, rectJ)
    val xOverlap = overlapRatio(rectI.left, rectI.right, rectJ.left, rectJ.right)
    val yOverlap = overlapRatio(rectI.top, rectI.bottom, rectJ.top, rectJ.bottom)
    val widthRatio = maxOf(rectI.width(), rectJ.width()).toFloat() /
        minOf(rectI.width(), rectJ.width()).coerceAtLeast(1).toFloat()
    val minHeight = minOf(rectI.height(), rectJ.height()).coerceAtLeast(1)
    val union = unionRects(listOf(rectI, rectJ)) ?: return false
    val fillRatio = mergedTextFillRatio(rectI, rectJ, union)

    val closeRows = verticalGap <= maxOf(averageThickness * settings.horizontalRowGapMultiplier, minHeight * 0.50f, 18f) ||
        yOverlap >= 0.36f
    val sameHorizontalBand = xOverlap >= settings.horizontalXOverlapThreshold ||
        (horizontalGap <= averageThickness * 1.4f && yOverlap >= 0.20f)
    val comparableWidth = widthRatio <= 3.0f
    val compactUnion = fillRatio >= settings.horizontalFillRatioMin

    return closeRows && sameHorizontalBand && comparableWidth && compactUnion
}

private fun mergeStrongOverlappingCandidates(candidates: List<OcrMergedCandidate>): List<OcrMergedCandidate> {
    val merged = candidates.toMutableList()

    var changed = true
    while (changed) {
        changed = false
        for (i in merged.indices) {
            for (j in i + 1 until merged.size) {
                if (shouldUnionStrongOverlap(merged[i].rect, merged[j].rect)) {
                    merged[i] = mergeCandidates(
                        candidateI = merged[i],
                        candidateJ = merged[j],
                        direction = mergedDirection(merged[i], merged[j])
                    )
                    merged.removeAt(j)
                    changed = true
                    break
                }
            }
            if (changed) break
        }
    }

    return merged
}

private fun shouldUnionStrongOverlap(rectI: Rect, rectJ: Rect): Boolean {
    val containment = maxOf(containmentRatio(rectI, rectJ), containmentRatio(rectJ, rectI))
    if (containment >= 0.72f) {
        return true
    }

    val iou = intersectionOverUnion(rectI, rectJ)
    if (iou >= 0.38f) {
        return true
    }

    val smallerOverlap = intersectionArea(rectI, rectJ) / minOf(rectI.area(), rectJ.area()).coerceAtLeast(1f)
    return smallerOverlap >= 0.62f && centerDistanceRatio(rectI, rectJ) <= 0.42f
}

private fun commonMergeDirection(
    candidateI: OcrMergedCandidate,
    candidateJ: OcrMergedCandidate
): OcrTextDirection? {
    return when {
        candidateI.direction == candidateJ.direction && candidateI.direction != OcrTextDirection.MIXED -> candidateI.direction
        candidateI.isVerticalLike() && candidateJ.isVerticalLike() -> OcrTextDirection.VERTICAL
        else -> null
    }
}

private fun mergedDirection(candidateI: OcrMergedCandidate, candidateJ: OcrMergedCandidate): OcrTextDirection {
    return commonMergeDirection(candidateI, candidateJ)
        ?: if (candidateI.rect.area() >= candidateJ.rect.area()) candidateI.direction else candidateJ.direction
}

private fun dedupeOverlappingCandidates(candidates: List<OcrMergedCandidate>): List<OcrMergedCandidate> {
    val result = mutableListOf<OcrMergedCandidate>()

    for (candidate in candidates.sortedWith(compareByDescending<OcrMergedCandidate> { it.rect.area() })) {
        var shouldAdd = true
        var indexToReplace = -1

        for (index in result.indices) {
            val existing = result[index]
            val duplicate = isDuplicateCandidate(candidate.rect, existing.rect)
            if (!duplicate) continue

            shouldAdd = false
            if (shouldPreferCandidate(candidate, existing)) {
                indexToReplace = index
            }
            break
        }

        if (indexToReplace >= 0) {
            result[indexToReplace] = candidate
        } else if (shouldAdd) {
            result.add(candidate)
        }
    }

    return result
}

private fun isDuplicateCandidate(rectI: Rect, rectJ: Rect): Boolean {
    val containment = maxOf(containmentRatio(rectI, rectJ), containmentRatio(rectJ, rectI))
    if (containment >= 0.82f) {
        return true
    }

    val iou = intersectionOverUnion(rectI, rectJ)
    if (iou >= 0.58f) {
        return true
    }

    val smallerOverlap = intersectionArea(rectI, rectJ) / minOf(rectI.area(), rectJ.area()).coerceAtLeast(1f)
    return smallerOverlap >= 0.72f && centerDistanceRatio(rectI, rectJ) <= 0.32f
}

private fun shouldPreferCandidate(candidate: OcrMergedCandidate, existing: OcrMergedCandidate): Boolean {
    val candidateArea = candidate.rect.area()
    val existingArea = existing.rect.area()
    return when {
        candidate.isVerticalLike() && !existing.isVerticalLike() -> true
        !candidate.isVerticalLike() && existing.isVerticalLike() -> false
        else -> candidateArea > existingArea
    }
}

private fun mergeCandidates(
    candidateI: OcrMergedCandidate,
    candidateJ: OcrMergedCandidate,
    direction: OcrTextDirection
): OcrMergedCandidate {
    val rect = unionRects(listOf(candidateI.rect, candidateJ.rect)) ?: candidateI.rect
    val thickness = medianOf(candidateI.averageLineThickness, candidateJ.averageLineThickness)
    return OcrMergedCandidate(
        rect = rect,
        direction = direction,
        averageLineThickness = thickness
    )
}

private fun OcrMergedCandidate.isVerticalLike(): Boolean {
    return direction == OcrTextDirection.VERTICAL ||
        (direction == OcrTextDirection.MIXED && rect.height() >= rect.width() * 1.15f)
}

private fun medianOf(valueI: Float, valueJ: Float): Float {
    return ((valueI + valueJ) / 2f).coerceAtLeast(1f)
}

private fun medianOrNull(values: List<Float>): Float? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    return sorted[sorted.size / 2].coerceAtLeast(1f)
}

private fun rhythmBaselineGap(values: List<Float>): Float? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val lowerHalfEnd = (sorted.size / 2).coerceAtLeast(1)
    return sorted.take(lowerHalfEnd).average().toFloat().coerceAtLeast(1f)
}

private fun detectTextDirection(lineRects: List<Rect>, fallbackRect: Rect): OcrTextDirection {
    if (lineRects.isEmpty()) {
        return if (fallbackRect.height() > fallbackRect.width() * 1.2f) {
            OcrTextDirection.VERTICAL
        } else {
            OcrTextDirection.HORIZONTAL
        }
    }

    val horizontalCount = lineRects.count { it.width() >= it.height() * 1.2f }
    val verticalCount = lineRects.count { it.height() >= it.width() * 1.2f }

    return when {
        horizontalCount > 0 && verticalCount == 0 -> OcrTextDirection.HORIZONTAL
        verticalCount > 0 && horizontalCount == 0 -> OcrTextDirection.VERTICAL
        horizontalCount >= verticalCount * 2 -> OcrTextDirection.HORIZONTAL
        verticalCount >= horizontalCount * 2 -> OcrTextDirection.VERTICAL
        else -> OcrTextDirection.MIXED
    }
}

private fun averageLineThickness(
    lineRects: List<Rect>,
    fallbackRect: Rect,
    direction: OcrTextDirection
): Float {
    val values = if (lineRects.isEmpty()) {
        listOf(lineThickness(fallbackRect, direction))
    } else {
        lineRects.map { lineThickness(it, direction) }
    }

    return values.sorted()[values.size / 2].coerceAtLeast(1f)
}

private fun lineThickness(rect: Rect, direction: OcrTextDirection): Float {
    return when (direction) {
        OcrTextDirection.HORIZONTAL -> rect.height().toFloat()
        OcrTextDirection.VERTICAL -> rect.width().toFloat()
        OcrTextDirection.MIXED -> minOf(rect.width(), rect.height()).toFloat()
    }
}

private fun unionRects(rects: List<Rect>): Rect? {
    if (rects.isEmpty()) return null

    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = Int.MIN_VALUE
    var bottom = Int.MIN_VALUE

    for (rect in rects) {
        left = minOf(left, rect.left)
        top = minOf(top, rect.top)
        right = maxOf(right, rect.right)
        bottom = maxOf(bottom, rect.bottom)
    }

    return Rect(left, top, right, bottom).takeIf { it.width() > 0 && it.height() > 0 }
}

private fun isPlausibleCandidate(
    candidate: OcrMergedCandidate,
    bitmapWidth: Int,
    bitmapHeight: Int
): Boolean {
    val rect = candidate.rect
    val width = rect.width()
    val height = rect.height()
    if (width <= 0 || height <= 0) return false

    val bitmapArea = (bitmapWidth * bitmapHeight).coerceAtLeast(1).toFloat()
    val areaRatio = rect.area() / bitmapArea
    val minDimension = minOf(width, height)
    val maxDimension = maxOf(width, height)
    val aspectRatio = maxDimension.toFloat() / minDimension.coerceAtLeast(1).toFloat()
    val touchesHorizontalEdge = rect.left <= bitmapWidth * 0.01f || rect.right >= bitmapWidth * 0.99f
    val touchesVerticalEdge = rect.top <= bitmapHeight * 0.01f || rect.bottom >= bitmapHeight * 0.99f

    if (areaRatio < 0.00018f) return false
    if (candidate.isVerticalLike()) {
        if (width < maxOf(8, (bitmapWidth * 0.008f).toInt())) return false
        if (height < maxOf(28, (bitmapHeight * 0.018f).toInt())) return false
        if (aspectRatio > 18f && width < candidate.averageLineThickness * 1.4f) return false
        if (touchesVerticalEdge && height < bitmapHeight * 0.08f) return false
    } else {
        if (height < maxOf(8, (bitmapHeight * 0.006f).toInt())) return false
        if (width < maxOf(32, (bitmapWidth * 0.025f).toInt())) return false
        if (aspectRatio > 22f && height < candidate.averageLineThickness * 1.4f) return false
        if (touchesHorizontalEdge && width < bitmapWidth * 0.08f) return false
    }

    val tooThinForLength = minDimension < 6 && maxDimension > minOf(bitmapWidth, bitmapHeight) * 0.12f
    return !tooThinForLength
}

private fun paddedRect(
    candidate: OcrMergedCandidate,
    bitmapWidth: Int,
    bitmapHeight: Int,
    settings: OcrBoxDetectionSettings
): Rect {
    val rect = candidate.rect
    val width = rect.width()
    val height = rect.height()
    val thickness = candidate.averageLineThickness.toInt().coerceAtLeast(1)
    val (paddingX, paddingY) = when {
        candidate.isVerticalLike() -> {
            val x = maxOf((width * maxOf(settings.verticalPaddingXRatio, 0.16f)).toInt(), (thickness * 0.9f).toInt(), 12)
            val y = maxOf((height * settings.verticalPaddingYRatio).toInt(), thickness, 14)
            x to y
        }
        candidate.direction == OcrTextDirection.HORIZONTAL -> {
            val x = maxOf((width * settings.horizontalPaddingXRatio).toInt(), thickness, 16)
            val y = maxOf((height * settings.horizontalPaddingYRatio).toInt(), (thickness * 0.55f).toInt(), 8)
            x to y
        }
        else -> {
            val x = maxOf((width * 0.12f).toInt(), 12)
            val y = maxOf((height * 0.12f).toInt(), 12)
            x to y
        }
    }

    return Rect(
        maxOf(0, rect.left - paddingX),
        maxOf(0, rect.top - paddingY),
        minOf(bitmapWidth, rect.right + paddingX),
        minOf(bitmapHeight, rect.bottom + paddingY)
    )
}

private fun Rect.area(): Float = width().coerceAtLeast(0) * height().coerceAtLeast(0).toFloat()

private fun intersectionArea(rectI: Rect, rectJ: Rect): Float {
    val left = maxOf(rectI.left, rectJ.left)
    val top = maxOf(rectI.top, rectJ.top)
    val right = minOf(rectI.right, rectJ.right)
    val bottom = minOf(rectI.bottom, rectJ.bottom)
    return maxOf(0, right - left) * maxOf(0, bottom - top).toFloat()
}

private fun intersectionOverUnion(rectI: Rect, rectJ: Rect): Float {
    val intersection = intersectionArea(rectI, rectJ)
    val union = rectI.area() + rectJ.area() - intersection
    return intersection / union.coerceAtLeast(1f)
}

private fun containmentRatio(inner: Rect, outer: Rect): Float {
    return intersectionArea(inner, outer) / inner.area().coerceAtLeast(1f)
}

private fun centerDistanceRatio(rectI: Rect, rectJ: Rect): Float {
    val centerDx = rectI.centerX() - rectJ.centerX()
    val centerDy = rectI.centerY() - rectJ.centerY()
    val distance = kotlin.math.sqrt((centerDx * centerDx + centerDy * centerDy).toFloat())
    val union = unionRects(listOf(rectI, rectJ)) ?: return 1f
    val diagonal = kotlin.math.sqrt((union.width() * union.width() + union.height() * union.height()).toFloat())
    return distance / diagonal.coerceAtLeast(1f)
}

private fun overlapRatio(startA: Int, endA: Int, startB: Int, endB: Int): Float {
    val overlap = maxOf(0, minOf(endA, endB) - maxOf(startA, startB)).toFloat()
    val smaller = minOf(endA - startA, endB - startB).coerceAtLeast(1).toFloat()
    return overlap / smaller
}

private fun mergedTextFillRatio(rectI: Rect, rectJ: Rect, union: Rect): Float {
    val textArea = rectI.area() + rectJ.area() - intersectionArea(rectI, rectJ)
    return textArea / union.area().coerceAtLeast(1f)
}

private fun mergeHybridBoxes(
    mlKitBoxes: List<Rect>,
    rapidRawBoxes: List<Rect>,
    bitmapWidth: Int,
    bitmapHeight: Int,
    settings: OcrBoxDetectionSettings
): List<Rect> {
    if (mlKitBoxes.isEmpty()) {
        return mergeRapidOcrFinalBoxes(rapidRawBoxes, bitmapWidth, bitmapHeight, settings)
    }
    if (rapidRawBoxes.isEmpty()) {
        return mlKitBoxes
    }

    val consumedRapidIndexes = mutableSetOf<Int>()
    val refinedMlKitBoxes = mlKitBoxes.map { mlKitBox ->
        val matches = rapidRawBoxes.withIndex()
            .filter { (_, rapidBox) -> shouldRapidBoxRefineMlKitBox(rapidBox, mlKitBox) }
        matches.forEach { (index, _) -> consumedRapidIndexes.add(index) }
        val matchedRects = matches.map { it.value }
        unionRects(listOf(mlKitBox) + matchedRects)
            ?.let { clampRect(it, bitmapWidth, bitmapHeight) }
            ?: mlKitBox
    }

    val orphanRapidBoxes = rapidRawBoxes.filterIndexed { index, _ -> index !in consumedRapidIndexes }
    val supplementalBoxes = buildHybridSupplementalRapidBoxes(
        boxes = orphanRapidBoxes,
        mlKitBoxes = refinedMlKitBoxes,
        bitmapWidth = bitmapWidth,
        bitmapHeight = bitmapHeight
    )

    return dedupeHybridBoxes(refinedMlKitBoxes + supplementalBoxes)
        .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
}

private fun shouldRapidBoxRefineMlKitBox(rapidBox: Rect, mlKitBox: Rect): Boolean {
    val expandedMlKit = expandRectForMatching(mlKitBox)
    val rapidArea = rapidBox.area().coerceAtLeast(1f)
    val mlKitArea = mlKitBox.area().coerceAtLeast(1f)
    val expandedMlKitArea = expandedMlKit.area().coerceAtLeast(1f)
    val union = unionRects(listOf(rapidBox, mlKitBox)) ?: return false
    val unionGrowth = union.area() / mlKitArea
    val rapidTooDominant = rapidArea > mlKitArea * 2.6f && containmentRatio(mlKitBox, rapidBox) < 0.58f
    if (rapidTooDominant || unionGrowth > 2.8f) return false

    val overlap = intersectionArea(rapidBox, expandedMlKit)
    if (overlap <= 0f) return false
    val overlapRapid = overlap / rapidArea
    val overlapMlKit = overlap / expandedMlKitArea

    val rapidCenterInside = rapidBox.centerX() in expandedMlKit.left..expandedMlKit.right &&
        rapidBox.centerY() in expandedMlKit.top..expandedMlKit.bottom
    if (rapidCenterInside && (overlapRapid >= 0.45f || overlapMlKit >= 0.08f)) return true

    val rapidInsideMlKit = overlapRapid >= 0.55f
    if (rapidInsideMlKit) return true

    return overlapRapid >= 0.34f && overlapMlKit >= 0.04f
}

private fun expandRectForMatching(rect: Rect): Rect {
    val padX = maxOf((rect.width() * 0.18f).toInt(), 10)
    val padY = maxOf((rect.height() * 0.16f).toInt(), 10)
    return Rect(
        rect.left - padX,
        rect.top - padY,
        rect.right + padX,
        rect.bottom + padY
    )
}

private fun clampRect(rect: Rect, bitmapWidth: Int, bitmapHeight: Int): Rect {
    return Rect(
        rect.left.coerceIn(0, bitmapWidth),
        rect.top.coerceIn(0, bitmapHeight),
        rect.right.coerceIn(0, bitmapWidth),
        rect.bottom.coerceIn(0, bitmapHeight)
    )
}

private fun isHybridDuplicate(rectI: Rect, rectJ: Rect): Boolean {
    return intersectionOverUnion(rectI, rectJ) >= 0.52f ||
        maxOf(containmentRatio(rectI, rectJ), containmentRatio(rectJ, rectI)) >= 0.78f
}

private fun buildHybridSupplementalRapidBoxes(
    boxes: List<Rect>,
    mlKitBoxes: List<Rect>,
    bitmapWidth: Int,
    bitmapHeight: Int
): List<Rect> {
    if (boxes.isEmpty()) return emptyList()

    val imageArea = (bitmapWidth * bitmapHeight).coerceAtLeast(1).toFloat()
    val candidates = boxes
        .asSequence()
        .filter { rect -> isPlausibleHybridSupplementalRapidBox(rect, imageArea, bitmapWidth, bitmapHeight) }
        .map { rect -> padHybridSupplementalRapidBox(rect, bitmapWidth, bitmapHeight) }
        .filter { rect ->
            mlKitBoxes.none { mlKitBox ->
                isHybridDuplicate(rect, mlKitBox) || isRapidSupplementTooCloseToMlKit(rect, mlKitBox)
            }
        }
        .toList()

    return dedupeHybridBoxes(candidates)
}

private fun isPlausibleHybridSupplementalRapidBox(
    rect: Rect,
    imageArea: Float,
    bitmapWidth: Int,
    bitmapHeight: Int
): Boolean {
    val width = rect.width()
    val height = rect.height()
    if (width <= 0 || height <= 0) return false

    val areaRatio = rect.area() / imageArea
    val minDimension = minOf(width, height)
    val maxDimension = maxOf(width, height)
    val aspectRatio = maxDimension.toFloat() / minDimension.coerceAtLeast(1)
    val verticalLike = height >= width * 1.25f

    if (areaRatio !in 0.00018f..0.055f) return false
    if (aspectRatio > 24f) return false
    if (verticalLike) {
        if (width < maxOf(7, (bitmapWidth * 0.006f).toInt())) return false
        if (height < maxOf(28, (bitmapHeight * 0.018f).toInt())) return false
    } else {
        if (height < maxOf(7, (bitmapHeight * 0.005f).toInt())) return false
        if (width < maxOf(28, (bitmapWidth * 0.020f).toInt())) return false
    }

    return !(minDimension <= 5 && maxDimension > minOf(bitmapWidth, bitmapHeight) * 0.10f)
}

private fun padHybridSupplementalRapidBox(
    rect: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int
): Rect {
    val verticalLike = rect.height() >= rect.width() * 1.25f
    val paddingX = if (verticalLike) maxOf((rect.width() * 0.14f).toInt(), 8) else maxOf((rect.width() * 0.04f).toInt(), 4)
    val paddingY = if (verticalLike) maxOf((rect.height() * 0.03f).toInt(), 3) else maxOf((rect.height() * 0.08f).toInt(), 4)
    return Rect(
        maxOf(0, rect.left - paddingX),
        maxOf(0, rect.top - paddingY),
        minOf(bitmapWidth, rect.right + paddingX),
        minOf(bitmapHeight, rect.bottom + paddingY)
    )
}

private fun isRapidSupplementTooCloseToMlKit(rapidBox: Rect, mlKitBox: Rect): Boolean {
    val expandedMlKit = expandRectForMatching(mlKitBox)
    val overlapRapid = containmentRatio(rapidBox, expandedMlKit)
    if (overlapRapid >= 0.22f) return true

    val centerAlignedX = overlapRatio(rapidBox.left, rapidBox.right, mlKitBox.left, mlKitBox.right) >= 0.45f
    val centerAlignedY = overlapRatio(rapidBox.top, rapidBox.bottom, mlKitBox.top, mlKitBox.bottom) >= 0.45f
    val closeVerticalNeighbor = centerAlignedX && gapY(rapidBox, mlKitBox) <= maxOf(rapidBox.height(), mlKitBox.height()) * 0.25f
    val closeHorizontalNeighbor = centerAlignedY && gapX(rapidBox, mlKitBox) <= maxOf(rapidBox.width(), mlKitBox.width()) * 0.25f
    return closeVerticalNeighbor || closeHorizontalNeighbor
}

private fun dedupeHybridBoxes(boxes: List<Rect>): List<Rect> {
    val result = mutableListOf<Rect>()
    for (box in boxes.sortedByDescending { it.area() }) {
        if (result.none { existing -> isHybridDuplicate(box, existing) }) {
            result.add(box)
        }
    }
    return result
}

private fun gapX(rectI: Rect, rectJ: Rect): Int {
    return when {
        rectI.right < rectJ.left -> rectJ.left - rectI.right
        rectJ.right < rectI.left -> rectI.left - rectJ.right
        else -> 0
    }
}

private fun gapY(rectI: Rect, rectJ: Rect): Int {
    return when {
        rectI.bottom < rectJ.top -> rectJ.top - rectI.bottom
        rectJ.bottom < rectI.top -> rectI.top - rectJ.bottom
        else -> 0
    }
}
