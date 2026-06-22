package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.model.OcrBoxPreviewMode
import java.nio.FloatBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

private const val RAPID_OCR_DET_MODEL_ASSET = "ocr/rapidocr/ch_PP-OCRv4_det_infer.onnx"
private const val RAPID_OCR_MIN_COMPONENT_PIXELS = 8

private object RapidOcrSessionHolder {
    private val lock = Any()
    private var session: OrtSession? = null

    fun getSession(context: Context): OrtSession {
        synchronized(lock) {
            session?.let { return it }
            val modelBytes = context.assets.open(RAPID_OCR_DET_MODEL_ASSET).use { input ->
                input.readBytes()
            }
            val created = OrtEnvironment.getEnvironment().createSession(modelBytes)
            session = created
            return created
        }
    }
}

suspend fun detectRapidOcrCameraBoxes(
    context: Context,
    bitmap: Bitmap,
    settings: OcrBoxDetectionSettings
): List<Rect> = withContext(Dispatchers.Default) {
    val rawBoxes = detectRapidOcrRawCameraBoxes(
        context = context,
        bitmap = bitmap,
        settings = settings
    )
    val normalized = settings.normalized()
    if (normalized.previewMode == OcrBoxPreviewMode.RAW) {
        rawBoxes
    } else {
        mergeRapidOcrFinalBoxes(
            boxes = rawBoxes,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            settings = normalized
        )
    }
}

suspend fun detectRapidOcrRawCameraBoxes(
    context: Context,
    bitmap: Bitmap,
    settings: OcrBoxDetectionSettings
): List<Rect> = withContext(Dispatchers.Default) {
    val normalized = settings.normalized()
    val prepared = prepareRapidOcrInput(bitmap, normalized.rapidOcrInputLongSide)
    val session = RapidOcrSessionHolder.getSession(context.applicationContext)
    val env = OrtEnvironment.getEnvironment()

    OnnxTensor.createTensor(env, FloatBuffer.wrap(prepared.input), longArrayOf(1L, 3L, prepared.inputHeight.toLong(), prepared.inputWidth.toLong())).use { tensor ->
        session.run(mapOf(session.inputNames.first() to tensor)).use { results ->
            val rawOutput = results.first().value
            val scoreMap = flattenRapidOcrOutput(rawOutput)
            val rawBoxes = rawRapidOcrBoxes(
                scoreMap = scoreMap.values,
                mapWidth = scoreMap.width,
                mapHeight = scoreMap.height,
                originalWidth = bitmap.width,
                originalHeight = bitmap.height,
                detThreshold = normalized.rapidOcrDetThreshold,
                boxThreshold = normalized.rapidOcrBoxThreshold,
                unclipRatio = normalized.rapidOcrUnclipRatio
            )
            rawBoxes
        }
    }
}

private data class RapidOcrPreparedInput(
    val input: FloatArray,
    val inputWidth: Int,
    val inputHeight: Int
)

private data class RapidOcrScoreMap(
    val values: FloatArray,
    val width: Int,
    val height: Int
)

private fun prepareRapidOcrInput(bitmap: Bitmap, longSide: Int): RapidOcrPreparedInput {
    val scale = minOf(
        longSide.toFloat() / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1),
        1f
    )
    val resizedWidth = alignTo32(maxOf(32, (bitmap.width * scale).toInt()))
    val resizedHeight = alignTo32(maxOf(32, (bitmap.height * scale).toInt()))
    val resized = Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
    val pixels = IntArray(resizedWidth * resizedHeight)
    resized.getPixels(pixels, 0, resizedWidth, 0, 0, resizedWidth, resizedHeight)
    if (resized !== bitmap && !resized.isRecycled) {
        resized.recycle()
    }

    val planeSize = resizedWidth * resizedHeight
    val input = FloatArray(planeSize * 3)
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val red = ((pixel shr 16) and 0xFF) / 255f
        val green = ((pixel shr 8) and 0xFF) / 255f
        val blue = (pixel and 0xFF) / 255f
        input[index] = (red - 0.485f) / 0.229f
        input[planeSize + index] = (green - 0.456f) / 0.224f
        input[planeSize * 2 + index] = (blue - 0.406f) / 0.225f
    }

    return RapidOcrPreparedInput(
        input = input,
        inputWidth = resizedWidth,
        inputHeight = resizedHeight
    )
}

private fun alignTo32(value: Int): Int {
    return maxOf(32, ((value + 31) / 32) * 32)
}

private fun flattenRapidOcrOutput(output: Any): RapidOcrScoreMap {
    return when (output) {
        is OnnxTensor -> {
            val info = output.info
            val elementCount = info.shape.fold(1L) { acc, value -> acc * value.coerceAtLeast(1L) }
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val buffer = output.floatBuffer
            val values = FloatArray(elementCount)
            buffer.rewind()
            buffer.get(values)
            val height = info.shape.getOrNull(info.shape.size - 2)
                ?.takeIf { it > 0L }
                ?.toInt()
                ?: 1
            val width = info.shape.getOrNull(info.shape.size - 1)
                ?.takeIf { it > 0L }
                ?.toInt()
                ?: values.size.coerceAtLeast(1)
            RapidOcrScoreMap(values = values, width = width, height = height)
        }
        is Array<*> -> {
            var current: Any? = output
            while (current is Array<*> && current.size == 1) {
                current = current.firstOrNull()
            }
            val values = when (current) {
                is Array<*> -> flattenNestedFloatArray(current)
                is FloatArray -> current
                else -> error("Unsupported RapidOCR output tensor shape: ${current?.javaClass?.name}")
            }
            RapidOcrScoreMap(values = values, width = values.size.coerceAtLeast(1), height = 1)
        }
        is FloatArray -> RapidOcrScoreMap(values = output, width = output.size.coerceAtLeast(1), height = 1)
        else -> error("Unsupported RapidOCR output tensor type: ${output.javaClass.name}")
    }
}

private fun flattenNestedFloatArray(value: Array<*>): FloatArray {
    val rows = value.filterIsInstance<FloatArray>()
    if (rows.isNotEmpty()) {
        return FloatArray(rows.sumOf { it.size }).also { out ->
            var offset = 0
            rows.forEach { row ->
                row.copyInto(out, offset)
                offset += row.size
            }
        }
    }

    val nested = value.filterIsInstance<Array<*>>()
    return FloatArray(nested.sumOf { flattenNestedFloatArray(it).size }).also { out ->
        var offset = 0
        nested.forEach { child ->
            val flat = flattenNestedFloatArray(child)
            flat.copyInto(out, offset)
            offset += flat.size
        }
    }
}

private fun rawRapidOcrBoxes(
    scoreMap: FloatArray,
    mapWidth: Int,
    mapHeight: Int,
    originalWidth: Int,
    originalHeight: Int,
    detThreshold: Float,
    boxThreshold: Float,
    unclipRatio: Float
): List<Rect> {
    if (scoreMap.size < mapWidth * mapHeight) {
        return emptyList()
    }

    val visited = BooleanArray(mapWidth * mapHeight)
    val boxes = mutableListOf<Rect>()
    val queueX = IntArray(mapWidth * mapHeight)
    val queueY = IntArray(mapWidth * mapHeight)

    for (y in 0 until mapHeight) {
        for (x in 0 until mapWidth) {
            val startIndex = y * mapWidth + x
            if (visited[startIndex] || scoreMap[startIndex] < detThreshold) continue

            var head = 0
            var tail = 0
            var left = x
            var right = x
            var top = y
            var bottom = y
            var sum = 0f
            var count = 0

            visited[startIndex] = true
            queueX[tail] = x
            queueY[tail] = y
            tail++

            while (head < tail) {
                val currentX = queueX[head]
                val currentY = queueY[head]
                head++

                val currentIndex = currentY * mapWidth + currentX
                sum += scoreMap[currentIndex]
                count++
                left = minOf(left, currentX)
                right = maxOf(right, currentX)
                top = minOf(top, currentY)
                bottom = maxOf(bottom, currentY)

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nextX = currentX + dx
                        val nextY = currentY + dy
                        if (nextX !in 0 until mapWidth || nextY !in 0 until mapHeight) continue
                        val nextIndex = nextY * mapWidth + nextX
                        if (visited[nextIndex] || scoreMap[nextIndex] < detThreshold) continue
                        visited[nextIndex] = true
                        queueX[tail] = nextX
                        queueY[tail] = nextY
                        tail++
                    }
                }
            }

            if (count < RAPID_OCR_MIN_COMPONENT_PIXELS || sum / count < boxThreshold) {
                continue
            }

            val scaleX = originalWidth.toFloat() / mapWidth
            val scaleY = originalHeight.toFloat() / mapHeight
            val width = (right - left + 1).coerceAtLeast(1)
            val height = (bottom - top + 1).coerceAtLeast(1)
            
            val distance = (width * height * unclipRatio) / (2f * (width + height))
            val inflateX = distance.coerceAtLeast(1f)
            val inflateY = distance.coerceAtLeast(1f)

            val rect = Rect(
                ((left - inflateX) * scaleX).toInt().coerceIn(0, originalWidth),
                ((top - inflateY) * scaleY).toInt().coerceIn(0, originalHeight),
                ((right + 1 + inflateX) * scaleX).toInt().coerceIn(0, originalWidth),
                ((bottom + 1 + inflateY) * scaleY).toInt().coerceIn(0, originalHeight)
            )
            if (rect.width() > 0 && rect.height() > 0) {
                boxes.add(rect)
            }
        }
    }

    return boxes.sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
}

private data class RapidOcrCandidate(
    val rect: Rect,
    val direction: RapidOcrDirection
)

private enum class RapidOcrDirection {
    VERTICAL,
    HORIZONTAL,
    MIXED
}

fun mergeRapidOcrFinalBoxes(
    boxes: List<Rect>,
    bitmapWidth: Int,
    bitmapHeight: Int,
    settings: OcrBoxDetectionSettings
): List<Rect> {
    if (boxes.isEmpty()) return emptyList()

    val filtered = filterRapidOcrNoise(boxes, bitmapWidth, bitmapHeight)
        .map { rect -> RapidOcrCandidate(rect = rect, direction = rapidOcrDirection(rect)) }

    val columnMerged = mergeRapidOcrCandidates(
        candidates = filtered,
        shouldMerge = ::shouldMergeSameVerticalColumn
    )
    val overlapMerged = mergeRapidOcrCandidates(
        candidates = columnMerged,
        shouldMerge = { first, second -> shouldMergeOverlappingRapidBoxes(first.rect, second.rect) }
    )

    return overlapMerged
        .map { candidate -> padRapidOcrFinalRect(candidate.rect, bitmapWidth, bitmapHeight, candidate.direction, settings) }
        .let { dedupeRapidOcrRects(it) }
        .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
}

private fun filterRapidOcrNoise(
    boxes: List<Rect>,
    bitmapWidth: Int,
    bitmapHeight: Int
): List<Rect> {
    val imageArea = (bitmapWidth * bitmapHeight).coerceAtLeast(1).toFloat()
    return boxes.filter { rect ->
        val width = rect.width()
        val height = rect.height()
        if (width <= 0 || height <= 0) return@filter false
        val areaRatio = width * height / imageArea
        val minDimension = minOf(width, height)
        val maxDimension = maxOf(width, height)
        val aspect = maxDimension.toFloat() / minDimension.coerceAtLeast(1)

        areaRatio >= 0.00003f &&
            minDimension >= 3 &&
            aspect <= 40f &&
            !(minDimension <= 4 && maxDimension > minOf(bitmapWidth, bitmapHeight) * 0.10f)
    }
}

private fun mergeRapidOcrCandidates(
    candidates: List<RapidOcrCandidate>,
    shouldMerge: (RapidOcrCandidate, RapidOcrCandidate) -> Boolean
): List<RapidOcrCandidate> {
    val merged = candidates.toMutableList()

    var changed = true
    while (changed) {
        changed = false
        for (i in merged.indices) {
            for (j in i + 1 until merged.size) {
                if (shouldMerge(merged[i], merged[j])) {
                    val rect = rapidUnionRects(merged[i].rect, merged[j].rect)
                    merged[i] = RapidOcrCandidate(
                        rect = rect,
                        direction = rapidMergedDirection(merged[i], merged[j], rect)
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

private fun shouldMergeSameVerticalColumn(
    first: RapidOcrCandidate,
    second: RapidOcrCandidate
): Boolean {
    if (!first.isVerticalLike() || !second.isVerticalLike()) return false
    val a = first.rect
    val b = second.rect
    val medianWidth = ((a.width() + b.width()) / 2f).coerceAtLeast(1f)
    val centerDistanceX = kotlin.math.abs(a.centerX() - b.centerX())
    val gapY = rapidGapY(a, b)
    val xOverlap = rapidOverlapRatio(a.left, a.right, b.left, b.right)
    val union = rapidUnionRects(a, b)
    val compactColumn = union.height() >= union.width() * 1.75f
    val similarWidth = maxOf(a.width(), b.width()).toFloat() /
        minOf(a.width(), b.width()).coerceAtLeast(1) <= 2.1f
    val notSeparatedByLargePanelGap = gapY <= maxOf(medianWidth * 3.0f, minOf(a.height(), b.height()) * 0.85f)

    return centerDistanceX <= medianWidth * 0.65f &&
        xOverlap >= 0.52f &&
        similarWidth &&
        notSeparatedByLargePanelGap &&
        compactColumn
}

private fun shouldMergeOverlappingRapidBoxes(a: Rect, b: Rect): Boolean {
    val containment = maxOf(rapidContainmentRatio(a, b), rapidContainmentRatio(b, a))
    if (containment >= 0.82f) return true
    val iou = rapidIntersectionOverUnion(a, b)
    return iou >= 0.56f
}

private fun padRapidOcrFinalRect(
    rect: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    direction: RapidOcrDirection,
    settings: OcrBoxDetectionSettings
): Rect {
    val width = rect.width()
    val height = rect.height()
    val (paddingX, paddingY) = when (direction) {
        RapidOcrDirection.VERTICAL -> {
            val x = maxOf((width * maxOf(settings.verticalPaddingXRatio, 0.16f)).toInt(), 10)
            val y = maxOf((height * maxOf(settings.verticalPaddingYRatio * 0.7f, 0.05f)).toInt(), 4)
            x to y
        }
        RapidOcrDirection.HORIZONTAL -> {
            val x = maxOf((width * maxOf(settings.horizontalPaddingXRatio * 0.7f, 0.06f)).toInt(), 6)
            val y = maxOf((height * maxOf(settings.horizontalPaddingYRatio, 0.12f)).toInt(), 5)
            x to y
        }
        RapidOcrDirection.MIXED -> {
            val x = maxOf((width * 0.10f).toInt(), 6)
            val y = maxOf((height * 0.10f).toInt(), 6)
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

private fun dedupeRapidOcrRects(rects: List<Rect>): List<Rect> {
    val result = mutableListOf<Rect>()
    for (rect in rects.sortedByDescending { it.width() * it.height() }) {
        val duplicateIndex = result.indexOfFirst { existing ->
            maxOf(rapidContainmentRatio(rect, existing), rapidContainmentRatio(existing, rect)) >= 0.86f ||
                rapidIntersectionOverUnion(rect, existing) >= 0.62f
        }
        if (duplicateIndex < 0) {
            result.add(rect)
        }
    }
    return result
}

private fun rapidOcrDirection(rect: Rect): RapidOcrDirection {
    return when {
        rect.height() >= rect.width() * 1.25f -> RapidOcrDirection.VERTICAL
        rect.width() >= rect.height() * 1.35f -> RapidOcrDirection.HORIZONTAL
        else -> RapidOcrDirection.MIXED
    }
}

private fun RapidOcrCandidate.isVerticalLike(): Boolean {
    return direction == RapidOcrDirection.VERTICAL ||
        (direction == RapidOcrDirection.MIXED && rect.height() >= rect.width())
}

private fun rapidMergedDirection(
    first: RapidOcrCandidate,
    second: RapidOcrCandidate,
    rect: Rect
): RapidOcrDirection {
    return when {
        first.direction == second.direction -> first.direction
        first.isVerticalLike() && second.isVerticalLike() -> RapidOcrDirection.VERTICAL
        else -> rapidOcrDirection(rect)
    }
}

private fun rapidUnionRects(a: Rect, b: Rect): Rect {
    return Rect(
        minOf(a.left, b.left),
        minOf(a.top, b.top),
        maxOf(a.right, b.right),
        maxOf(a.bottom, b.bottom)
    )
}

private fun rapidGapX(a: Rect, b: Rect): Int {
    return when {
        a.right < b.left -> b.left - a.right
        b.right < a.left -> a.left - b.right
        else -> 0
    }
}

private fun rapidGapY(a: Rect, b: Rect): Int {
    return when {
        a.bottom < b.top -> b.top - a.bottom
        b.bottom < a.top -> a.top - b.bottom
        else -> 0
    }
}

private fun rapidOverlapRatio(startA: Int, endA: Int, startB: Int, endB: Int): Float {
    val overlap = maxOf(0, minOf(endA, endB) - maxOf(startA, startB)).toFloat()
    val smaller = minOf(endA - startA, endB - startB).coerceAtLeast(1).toFloat()
    return overlap / smaller
}

private fun rapidIntersectionArea(a: Rect, b: Rect): Float {
    val left = maxOf(a.left, b.left)
    val top = maxOf(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)
    return maxOf(0, right - left) * maxOf(0, bottom - top).toFloat()
}

private fun rapidContainmentRatio(inner: Rect, outer: Rect): Float {
    return rapidIntersectionArea(inner, outer) / (inner.width() * inner.height()).coerceAtLeast(1).toFloat()
}

private fun rapidIntersectionOverUnion(a: Rect, b: Rect): Float {
    val intersection = rapidIntersectionArea(a, b)
    val union = (a.width() * a.height() + b.width() * b.height()).toFloat() - intersection
    return intersection / union.coerceAtLeast(1f)
}
