package com.example.japanesegrammarapp.ui.screens

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.magnifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.utils.AppLogger

enum class CropInteraction {
    AREA_CROP,
    TEXT_SELECT
}

@Composable
fun SegmentedControl(
    selected: CropInteraction,
    onSelected: (CropInteraction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        val areaBg = if (selected == CropInteraction.AREA_CROP) Color.White.copy(alpha = 0.2f) else Color.Transparent
        val textBg = if (selected == CropInteraction.TEXT_SELECT) Color.White.copy(alpha = 0.2f) else Color.Transparent
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(areaBg, RoundedCornerShape(6.dp))
                .clickable { onSelected(CropInteraction.AREA_CROP) },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.crop_interaction_area), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(textBg, RoundedCornerShape(6.dp))
                .clickable { onSelected(CropInteraction.TEXT_SELECT) },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.crop_interaction_text_select), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun ImageCropReviewLayout(
    bitmap: Bitmap,
    ocrBoxDetectionSettings: OcrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val cropState = remember(bitmap) {
        CropState(
            bitmapWidth = bitmap.width.toFloat(),
            bitmapHeight = bitmap.height.toFloat()
        )
    }

    var interactionMode by remember { mutableStateOf(CropInteraction.AREA_CROP) }
    var textSelectStart by remember { mutableStateOf<TextHandleState?>(null) }
    var textSelectEnd by remember { mutableStateOf<TextHandleState?>(null) }
    var magnifierCenter by remember { mutableStateOf<Offset?>(null) }

    // When the screen orientation changes, the container size changes too.
    // Reset isInitialized so the crop box is recalculated for the new layout.
    LaunchedEffect(isLandscape) {
        cropState.isInitialized = false
    }

    var detectedBoxes by remember(bitmap, ocrBoxDetectionSettings) { mutableStateOf<List<Rect>>(emptyList()) }
    var hideOcrBoxes by remember { mutableStateOf(false) }

    LaunchedEffect(bitmap, ocrBoxDetectionSettings, interactionMode) {
        try {
            if (interactionMode == CropInteraction.AREA_CROP) {
                val mergedBoxes = detectCameraOcrBoxes(
                    bitmap = bitmap,
                    settings = ocrBoxDetectionSettings,
                    context = context
                )
                detectedBoxes = mergedBoxes
                hideOcrBoxes = mergedBoxes.isEmpty()
            } else {
                val fineBoxes = detectFineGrainedCameraOcrBoxes(
                    bitmap = bitmap,
                    settings = ocrBoxDetectionSettings,
                    context = context
                )
                detectedBoxes = fineBoxes
                hideOcrBoxes = false
                textSelectStart = null
                textSelectEnd = null
            }
        } catch (e: Exception) {
            AppLogger.e("CAMERA", "Failed to detect OCR text boxes", e)
        }
    }

    fun confirmBitmapRegion(x: Int, y: Int, w: Int, h: Int) {
        if (w > 0 && h > 0) {
            try {
                onConfirm(Bitmap.createBitmap(bitmap, x, y, w, h))
            } catch (e: Throwable) {
                AppLogger.e("CAMERA", "Failed to crop selected bitmap region", e)
                onConfirm(bitmap)
            }
        } else {
            onConfirm(bitmap)
        }
    }

    fun confirmOcrBox(box: Rect) {
        val x = box.left.coerceIn(0, bitmap.width)
        val y = box.top.coerceIn(0, bitmap.height)
        val w = minOf(bitmap.width - x, box.width())
        val h = minOf(bitmap.height - y, box.height())
        confirmBitmapRegion(x, y, w, h)
    }

    fun confirmCurrentCrop() {
        val bmpW = bitmap.width
        val bmpH = bitmap.height

        val x = ((cropState.cropLeft - cropState.imgOffsetX) / cropState.scaleFactor).toInt().coerceIn(0, bmpW)
        val y = ((cropState.cropTop - cropState.imgOffsetY) / cropState.scaleFactor).toInt().coerceIn(0, bmpH)

        var w = ((cropState.cropRight - cropState.cropLeft) / cropState.scaleFactor).toInt()
        var h = ((cropState.cropBottom - cropState.cropTop) / cropState.scaleFactor).toInt()

        if (x + w > bmpW) w = bmpW - x
        if (y + h > bmpH) h = bmpH - y

        confirmBitmapRegion(x, y, w, h)
    }
    
    @Composable
    fun WorkspaceArea(modifier: Modifier) {
        Box(
            modifier = modifier
                .onGloballyPositioned { layoutCoordinates ->
                    cropState.initializeCropBox(
                        layoutCoordinates.size.width.toFloat(),
                        layoutCoordinates.size.height.toFloat()
                    )
                }
        ) {
            if (cropState.isInitialized) {
                // Renders the original full-size image scaled to fit
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw original scaled image
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(cropState.imgOffsetX.toInt(), cropState.imgOffsetY.toInt()),
                        dstSize = IntSize(cropState.imgDispWidth.toInt(), cropState.imgDispHeight.toInt())
                    )
                }
                
                val minSizePx = with(density) { 16.dp.toPx() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .run {
                            if (magnifierCenter != null) {
                                this.magnifier(
                                    sourceCenter = { magnifierCenter ?: androidx.compose.ui.geometry.Offset.Unspecified },
                                    magnifierCenter = {
                                        val center = magnifierCenter
                                        if (center != null) {
                                            androidx.compose.ui.geometry.Offset(center.x, center.y - 100.dp.toPx())
                                        } else {
                                            androidx.compose.ui.geometry.Offset.Unspecified
                                        }
                                    },
                                    zoom = 1.5f
                                )
                            } else this
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val downEvent = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = downEvent.position
                                    var activePointerId = downEvent.id
                                    
                                    val minTolerancePx = 32.dp.toPx() // Corner grab tolerance
                                    val dragSlopPx = 8.dp.toPx() // Drag vs tap threshold
                                    val longPressMs = 350L
                                    
                                    var targetBox: Rect? = null
                                    var targetHandle = DragHandle.NONE
                                    var submitTarget: (() -> Unit)? = null
                                    var activeTextHandle: String? = null
                                    cropState.activeHandle = DragHandle.NONE
                                    
                                    if (interactionMode == CropInteraction.TEXT_SELECT) {
                                        if (detectedBoxes.isNotEmpty()) {
                                            val getHandlePos = { handle: TextHandleState, isStart: Boolean -> 
                                                val box = detectedBoxes.getOrNull(handle.index)
                                                if (box != null) {
                                                    val isVertical = box.height() > box.width()
                                                    val R = 10.dp.toPx()
                                                    val D = R * 1.414f
                                                    if (isVertical) {
                                                        val y = cropState.imgOffsetY + (box.top + box.height() * handle.offsetRatio) * cropState.scaleFactor
                                                        if (isStart) {
                                                            val x = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                                            Offset(x - D, y)
                                                        } else {
                                                            val x = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                                            Offset(x + D, y)
                                                        }
                                                    } else {
                                                        val x = cropState.imgOffsetX + (box.left + box.width() * handle.offsetRatio) * cropState.scaleFactor
                                                        val y = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                                        Offset(x, y + D)
                                                    }
                                                } else {
                                                    Offset.Zero
                                                }
                                            }

                                            val touchTolerance = 64.dp.toPx()
                                            val startCenter = textSelectStart?.let { getHandlePos(it, true) } ?: Offset.Zero
                                            val endCenter = textSelectEnd?.let { getHandlePos(it, false) } ?: Offset.Zero
                                            
                                            val distStart = distance(startPos.x, startPos.y, startCenter.x, startCenter.y)
                                            val distEnd = distance(startPos.x, startPos.y, endCenter.x, endCenter.y)
                                            
                                            var handleGrabbed = false
                                            if (textSelectStart != null && textSelectEnd != null) {
                                                if (distStart < distEnd && distStart < touchTolerance) {
                                                    activeTextHandle = "START"
                                                    handleGrabbed = true
                                                } else if (distEnd < touchTolerance) {
                                                    activeTextHandle = "END"
                                                    handleGrabbed = true
                                                }
                                            }

                                            if (!handleGrabbed) {
                                                val hitTolerance = 16.dp.toPx()
                                                val closestIdx = detectedBoxes.indices.minByOrNull { i ->
                                                    val box = detectedBoxes[i]
                                                    val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                                    val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                                                    val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                                    val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                                    
                                                    val dx = maxOf(0f, maxOf(displayLeft - startPos.x, startPos.x - displayRight))
                                                    val dy = maxOf(0f, maxOf(displayTop - startPos.y, startPos.y - displayBottom))
                                                    dx * dx + dy * dy
                                                }
                                                val closestDistSq = closestIdx?.let { i ->
                                                    val box = detectedBoxes[i]
                                                    val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                                    val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                                                    val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                                    val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                                    
                                                    val dx = maxOf(0f, maxOf(displayLeft - startPos.x, startPos.x - displayRight))
                                                    val dy = maxOf(0f, maxOf(displayTop - startPos.y, startPos.y - displayBottom))
                                                    dx * dx + dy * dy
                                                } ?: Float.MAX_VALUE

                                                if (closestIdx != null && closestDistSq <= hitTolerance * hitTolerance) {
                                                    textSelectStart = TextHandleState(closestIdx, 0f)
                                                    textSelectEnd = TextHandleState(closestIdx, 1f)
                                                    activeTextHandle = "END"
                                                } else {
                                                    textSelectStart = null
                                                    textSelectEnd = null
                                                    activeTextHandle = null
                                                }
                                            }
                                        }
                                        submitTarget = { /* no op for text select tap */ }
                                    } else if (!hideOcrBoxes) {
                                        var minDist = Float.MAX_VALUE
                                        
                                        for (box in detectedBoxes) {
                                            val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                            val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                                            val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                            val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                            
                                            val distTL = distance(startPos.x, startPos.y, displayLeft, displayTop)
                                            val distTR = distance(startPos.x, startPos.y, displayRight, displayTop)
                                            val distBL = distance(startPos.x, startPos.y, displayLeft, displayBottom)
                                            val distBR = distance(startPos.x, startPos.y, displayRight, displayBottom)
                                            
                                            if (distTL < minDist && distTL < minTolerancePx) { minDist = distTL; targetBox = box; targetHandle = DragHandle.TOP_LEFT }
                                            if (distTR < minDist && distTR < minTolerancePx) { minDist = distTR; targetBox = box; targetHandle = DragHandle.TOP_RIGHT }
                                            if (distBL < minDist && distBL < minTolerancePx) { minDist = distBL; targetBox = box; targetHandle = DragHandle.BOTTOM_LEFT }
                                            if (distBR < minDist && distBR < minTolerancePx) { minDist = distBR; targetBox = box; targetHandle = DragHandle.BOTTOM_RIGHT }

                                            if (submitTarget == null && startPos.x in displayLeft..displayRight && startPos.y in displayTop..displayBottom) {
                                                submitTarget = { confirmOcrBox(box) }
                                            }
                                        }

                                        if (targetBox != null && submitTarget == null) {
                                            val box = targetBox
                                            submitTarget = { confirmOcrBox(box) }
                                        }
                                    } else {
                                        val tolerance = with(density) {
                                            val boxWidth = cropState.cropRight - cropState.cropLeft
                                            val boxHeight = cropState.cropBottom - cropState.cropTop
                                            val minBoxDimension = minOf(boxWidth, boxHeight)
                                            minOf(48.dp.toPx(), minBoxDimension * 0.5f).coerceAtLeast(16.dp.toPx())
                                        }
                                        cropState.startDrag(startPos, tolerance)
                                        targetHandle = cropState.activeHandle
                                        cropState.activeHandle = DragHandle.NONE
                                    }
                                    
                                    if (submitTarget == null) {
                                        submitTarget = { confirmCurrentCrop() }
                                    }

                                    var isEditing = false
                                    val canEdit = targetHandle != DragHandle.NONE || activeTextHandle != null

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val changes = event.changes

                                        if (changes.all { !it.pressed }) {
                                            magnifierCenter = null
                                            if (isEditing) {
                                                if (interactionMode == CropInteraction.AREA_CROP) {
                                                    cropState.stopDrag()
                                                }
                                            } else {
                                                downEvent.consume()
                                                submitTarget?.invoke()
                                            }
                                            break
                                        }

                                        val activeChanges = changes.filter { it.pressed }
                                        if (activeChanges.size >= 2) {
                                            if (isEditing) {
                                                activeChanges.forEach { it.consume() }
                                            }
                                            continue
                                        }

                                        val currentFinger = activeChanges.find { it.id == activePointerId } ?: activeChanges.firstOrNull()
                                        if (currentFinger != null) {
                                            activePointerId = currentFinger.id

                                            if (interactionMode == CropInteraction.TEXT_SELECT) {
                                                if (activeTextHandle != null) {
                                                    isEditing = true
                                                    val currentPos = currentFinger.position
                                                    magnifierCenter = currentPos
                                                    
                                                    val otherHandle = if (activeTextHandle == "START") textSelectEnd else textSelectStart
                                                    val targetOrientationIsVertical = otherHandle?.let {
                                                        val b = detectedBoxes.getOrNull(it.index)
                                                        b != null && b.height() > b.width()
                                                    }

                                                    val allowedIndices = if (targetOrientationIsVertical != null) {
                                                        detectedBoxes.indices.filter { i ->
                                                            val b = detectedBoxes[i]
                                                            (b.height() > b.width()) == targetOrientationIsVertical
                                                        }
                                                    } else detectedBoxes.indices

                                                    val closestIdx = allowedIndices.minByOrNull { i ->
                                                        val box = detectedBoxes[i]
                                                        val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                                        val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                                                        val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                                        val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                                        
                                                        val dx = maxOf(0f, maxOf(displayLeft - currentPos.x, currentPos.x - displayRight))
                                                        val dy = maxOf(0f, maxOf(displayTop - currentPos.y, currentPos.y - displayBottom))
                                                        val boundsDistSq = dx * dx + dy * dy
                                                        
                                                        val cx = (displayLeft + displayRight) / 2f
                                                        val cy = (displayTop + displayBottom) / 2f
                                                        val centerDist = distance(currentPos.x, currentPos.y, cx, cy)
                                                        
                                                        boundsDistSq + centerDist * 0.001f
                                                    } ?: (otherHandle?.index ?: 0)
                                                    
                                                    val closestBox = detectedBoxes[closestIdx]
                                                    val isVertical = closestBox.height() > closestBox.width()
                                                    val offsetRatio = if (isVertical) {
                                                        val displayTop = cropState.imgOffsetY + closestBox.top * cropState.scaleFactor
                                                        val displayHeight = closestBox.height() * cropState.scaleFactor
                                                        if (displayHeight > 0) ((currentPos.y - displayTop) / displayHeight).coerceIn(0f, 1f) else 0f
                                                    } else {
                                                        val displayLeft = cropState.imgOffsetX + closestBox.left * cropState.scaleFactor
                                                        val displayWidth = closestBox.width() * cropState.scaleFactor
                                                        if (displayWidth > 0) ((currentPos.x - displayLeft) / displayWidth).coerceIn(0f, 1f) else 0f
                                                    }
                                                    
                                                    if (activeTextHandle == "START") {
                                                        textSelectStart = TextHandleState(closestIdx, offsetRatio)
                                                    } else {
                                                        textSelectEnd = TextHandleState(closestIdx, offsetRatio)
                                                    }
                                                    currentFinger.consume()
                                                }
                                            } else if (!isEditing && canEdit) {
                                                val currentPos = currentFinger.position
                                                val totalDelta = currentPos - startPos
                                                val dist = kotlin.math.sqrt(totalDelta.x * totalDelta.x + totalDelta.y * totalDelta.y)
                                                val heldLongEnough = currentFinger.uptimeMillis - downEvent.uptimeMillis >= longPressMs

                                                if (heldLongEnough && dist > dragSlopPx) {
                                                    targetBox?.let { box ->
                                                        val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                                        val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                                                        val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                                        val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor

                                                        cropState.cropLeft = displayLeft
                                                        cropState.cropTop = displayTop
                                                        cropState.cropRight = displayRight
                                                        cropState.cropBottom = displayBottom
                                                        hideOcrBoxes = true
                                                    }
                                                    cropState.activeHandle = targetHandle
                                                    downEvent.consume()
                                                    currentFinger.consume()
                                                    cropState.onDrag(currentFinger.position - currentFinger.previousPosition, minSizePx)
                                                    isEditing = true
                                                }
                                            } else if (isEditing) {
                                                currentFinger.consume()
                                                cropState.onDrag(currentFinger.position - currentFinger.previousPosition, minSizePx)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    if (interactionMode == CropInteraction.TEXT_SELECT) {
                        if (detectedBoxes.isNotEmpty()) {
                            val imageLeft = cropState.imgOffsetX
                            val imageTop = cropState.imgOffsetY
                            val imageRight = cropState.imgOffsetX + cropState.imgDispWidth
                            val imageBottom = cropState.imgOffsetY + cropState.imgDispHeight

                            // Draw shadow with holes for all selectable text
                            val highlightedRects = detectedBoxes.mapNotNull { box ->
                                val displayLeft = (cropState.imgOffsetX + box.left * cropState.scaleFactor).coerceIn(imageLeft, imageRight)
                                val displayTop = (cropState.imgOffsetY + box.top * cropState.scaleFactor).coerceIn(imageTop, imageBottom)
                                val displayRight = (cropState.imgOffsetX + box.right * cropState.scaleFactor).coerceIn(imageLeft, imageRight)
                                val displayBottom = (cropState.imgOffsetY + box.bottom * cropState.scaleFactor).coerceIn(imageTop, imageBottom)

                                if (displayRight > displayLeft && displayBottom > displayTop) {
                                    ComposeRect(displayLeft, displayTop, displayRight, displayBottom)
                                } else {
                                    null
                                }
                            }

                            drawContext.canvas.saveLayer(ComposeRect(imageLeft, imageTop, imageRight, imageBottom), Paint())
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(imageLeft, imageTop),
                                size = Size(imageRight - imageLeft, imageBottom - imageTop)
                            )
                            highlightedRects.forEach { rect ->
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(rect.left, rect.top),
                                    size = Size(rect.width, rect.height),
                                    blendMode = BlendMode.Clear
                                )
                            }
                            drawContext.canvas.restore()

                            if (textSelectStart != null && textSelectEnd != null) {
                                val startHandle = textSelectStart!!
                                val endHandle = textSelectEnd!!
                                
                                val (actualStart, actualEnd) = getOrderedHandles(startHandle, endHandle)
                                val slicedBoxes = calculateSubLineRects(detectedBoxes, actualStart, actualEnd)
                                
                                slicedBoxes.forEach { box ->
                                    val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                    val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                                    val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                    val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                    
                                    drawRect(
                                        color = KuriAmber.copy(alpha = 0.35f),
                                        topLeft = Offset(displayLeft, displayTop),
                                        size = Size(displayRight - displayLeft, displayBottom - displayTop)
                                    )
                                    drawRect(
                                        color = KuriAmber,
                                        topLeft = Offset(displayLeft, displayTop),
                                        size = Size(displayRight - displayLeft, displayBottom - displayTop),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                    )
                                }
                                
                                val drawTeardrop = { handle: TextHandleState, isStart: Boolean -> 
                                    val box = detectedBoxes.getOrNull(handle.index)
                                    if (box != null) {
                                        val isVertical = box.height() > box.width()
                                        val path = androidx.compose.ui.graphics.Path()
                                        val R = 10.dp.toPx()
                                        val D = R * 1.414f
                                        var cx = 0f
                                        var cy = 0f
                                        
                                        if (isVertical) {
                                            val y = cropState.imgOffsetY + (box.top + box.height() * handle.offsetRatio) * cropState.scaleFactor
                                            if (isStart) {
                                                val x = cropState.imgOffsetX + box.left * cropState.scaleFactor
                                                cx = x - D
                                                cy = y
                                                path.moveTo(x, y)
                                                path.lineTo(cx + R * 0.707f, cy + R * 0.707f)
                                                path.arcTo(
                                                    rect = ComposeRect(cx - R, cy - R, cx + R, cy + R),
                                                    startAngleDegrees = 45f,
                                                    sweepAngleDegrees = 270f,
                                                    forceMoveTo = false
                                                )
                                                path.close()
                                            } else {
                                                val x = cropState.imgOffsetX + box.right * cropState.scaleFactor
                                                cx = x + D
                                                cy = y
                                                path.moveTo(x, y)
                                                path.lineTo(cx - R * 0.707f, cy - R * 0.707f)
                                                path.arcTo(
                                                    rect = ComposeRect(cx - R, cy - R, cx + R, cy + R),
                                                    startAngleDegrees = 225f,
                                                    sweepAngleDegrees = 270f,
                                                    forceMoveTo = false
                                                )
                                                path.close()
                                            }
                                        } else {
                                            val x = cropState.imgOffsetX + (box.left + box.width() * handle.offsetRatio) * cropState.scaleFactor
                                            val y = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                                            cx = x
                                            cy = y + D
                                            path.moveTo(x, y)
                                            path.lineTo(cx + R * 0.707f, cy - R * 0.707f)
                                            path.arcTo(
                                                rect = ComposeRect(cx - R, cy - R, cx + R, cy + R),
                                                startAngleDegrees = 315f,
                                                sweepAngleDegrees = 270f,
                                                forceMoveTo = false
                                            )
                                            path.close()
                                        }
                                        
                                        drawPath(path, color = Color.White)
                                        drawCircle(color = KuriAmber, radius = 6.dp.toPx(), center = Offset(cx, cy))
                                    }
                                }
                                
                                drawTeardrop(startHandle, true)
                                drawTeardrop(endHandle, false)
                            }
                        }
                    } else if (hideOcrBoxes) {
                        // Draw dimming mask outside crop box bounds
                        // Top mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(cropState.imgOffsetX, cropState.imgOffsetY),
                            size = Size(cropState.imgDispWidth, cropState.cropTop - cropState.imgOffsetY)
                        )
                        // Bottom mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(cropState.imgOffsetX, cropState.cropBottom),
                            size = Size(cropState.imgDispWidth, cropState.imgOffsetY + cropState.imgDispHeight - cropState.cropBottom)
                        )
                        // Left mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(cropState.imgOffsetX, cropState.cropTop),
                            size = Size(cropState.cropLeft - cropState.imgOffsetX, cropState.cropBottom - cropState.cropTop)
                        )
                        // Right mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(cropState.cropRight, cropState.cropTop),
                            size = Size(cropState.imgOffsetX + cropState.imgDispWidth - cropState.cropRight, cropState.cropBottom - cropState.cropTop)
                        )
                        
                        // Draw elegant crop frame borders
                        val borderStrokeW = 2.dp.toPx()
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(cropState.cropLeft, cropState.cropTop),
                            size = Size(cropState.cropRight - cropState.cropLeft, cropState.cropBottom - cropState.cropTop),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderStrokeW)
                        )
                        
                        // Auxiliary grid lines (Rule of Thirds style inside crop frame)
                        val frameW = cropState.cropRight - cropState.cropLeft
                        val frameH = cropState.cropBottom - cropState.cropTop
                        
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropState.cropLeft + frameW / 3f, cropState.cropTop),
                            end = Offset(cropState.cropLeft + frameW / 3f, cropState.cropBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropState.cropLeft + (frameW * 2f) / 3f, cropState.cropTop),
                            end = Offset(cropState.cropLeft + (frameW * 2f) / 3f, cropState.cropBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropState.cropLeft, cropState.cropTop + frameH / 3f),
                            end = Offset(cropState.cropRight, cropState.cropTop + frameH / 3f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropState.cropLeft, cropState.cropTop + (frameH * 2f) / 3f),
                            end = Offset(cropState.cropRight, cropState.cropTop + (frameH * 2f) / 3f),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Draw 4 handles (Japanese-inspired accent circles)
                        val handleRadius = 7.dp.toPx()
                        val activeColor = KuriAmber
                        val defaultColor = Color.White
                        
                        // TL
                        drawCircle(
                            color = if (cropState.activeHandle == DragHandle.TOP_LEFT) activeColor else defaultColor,
                            radius = handleRadius,
                            center = Offset(cropState.cropLeft, cropState.cropTop)
                        )
                        // TR
                        drawCircle(
                            color = if (cropState.activeHandle == DragHandle.TOP_RIGHT) activeColor else defaultColor,
                            radius = handleRadius,
                            center = Offset(cropState.cropRight, cropState.cropTop)
                        )
                        // BL
                        drawCircle(
                            color = if (cropState.activeHandle == DragHandle.BOTTOM_LEFT) activeColor else defaultColor,
                            radius = handleRadius,
                            center = Offset(cropState.cropLeft, cropState.cropBottom)
                        )
                        // BR
                        drawCircle(
                            color = if (cropState.activeHandle == DragHandle.BOTTOM_RIGHT) activeColor else defaultColor,
                            radius = handleRadius,
                            center = Offset(cropState.cropRight, cropState.cropBottom)
                        )
                    } else {
                        if (detectedBoxes.isNotEmpty()) {
                            val imageLeft = cropState.imgOffsetX
                            val imageTop = cropState.imgOffsetY
                            val imageRight = cropState.imgOffsetX + cropState.imgDispWidth
                            val imageBottom = cropState.imgOffsetY + cropState.imgDispHeight
                            val highlightedRects = detectedBoxes.mapNotNull { box ->
                                val displayLeft = (cropState.imgOffsetX + box.left * cropState.scaleFactor).coerceIn(imageLeft, imageRight)
                                val displayTop = (cropState.imgOffsetY + box.top * cropState.scaleFactor).coerceIn(imageTop, imageBottom)
                                val displayRight = (cropState.imgOffsetX + box.right * cropState.scaleFactor).coerceIn(imageLeft, imageRight)
                                val displayBottom = (cropState.imgOffsetY + box.bottom * cropState.scaleFactor).coerceIn(imageTop, imageBottom)

                                if (displayRight > displayLeft && displayBottom > displayTop) {
                                    ComposeRect(displayLeft, displayTop, displayRight, displayBottom)
                                } else {
                                    null
                                }
                            }

                            drawContext.canvas.saveLayer(ComposeRect(imageLeft, imageTop, imageRight, imageBottom), Paint())
                            drawRect(
                                color = Color.Black.copy(alpha = 0.24f),
                                topLeft = Offset(imageLeft, imageTop),
                                size = Size(imageRight - imageLeft, imageBottom - imageTop)
                            )
                            highlightedRects.forEach { rect ->
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(rect.left, rect.top),
                                    size = Size(rect.width, rect.height),
                                    blendMode = BlendMode.Clear
                                )
                            }
                            drawContext.canvas.restore()

                            highlightedRects.forEach { rect ->
                                val displayLeft = rect.left
                                val displayTop = rect.top
                                val displayRight = rect.right
                                val displayBottom = rect.bottom
                                val frameWidth = displayRight - displayLeft
                                val frameHeight = displayBottom - displayTop

                                if (frameWidth <= 0f || frameHeight <= 0f) {
                                    return@forEach
                                }

                                val haloStroke = 5.dp.toPx()
                                val shadowStroke = 3.5.dp.toPx()
                                val borderStroke = 2.dp.toPx()
                                val cornerLength = minOf(12.dp.toPx(), frameWidth / 2f, frameHeight / 2f)
                                val cornerStroke = 2.dp.toPx()
                                val selectionColor = Color.White

                                drawRect(
                                    color = selectionColor.copy(alpha = 0.20f),
                                    topLeft = Offset(displayLeft, displayTop),
                                    size = Size(frameWidth, frameHeight),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = haloStroke)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    topLeft = Offset(displayLeft, displayTop),
                                    size = Size(frameWidth, frameHeight),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = shadowStroke)
                                )

                                drawRect(
                                    color = selectionColor,
                                    topLeft = Offset(displayLeft, displayTop),
                                    size = Size(frameWidth, frameHeight),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderStroke)
                                )

                                // TL
                                drawLine(selectionColor, Offset(displayLeft, displayTop), Offset(displayLeft + cornerLength, displayTop), strokeWidth = cornerStroke)
                                drawLine(selectionColor, Offset(displayLeft, displayTop), Offset(displayLeft, displayTop + cornerLength), strokeWidth = cornerStroke)

                                // TR
                                drawLine(selectionColor, Offset(displayRight, displayTop), Offset(displayRight - cornerLength, displayTop), strokeWidth = cornerStroke)
                                drawLine(selectionColor, Offset(displayRight, displayTop), Offset(displayRight, displayTop + cornerLength), strokeWidth = cornerStroke)

                                // BL
                                drawLine(selectionColor, Offset(displayLeft, displayBottom), Offset(displayLeft + cornerLength, displayBottom), strokeWidth = cornerStroke)
                                drawLine(selectionColor, Offset(displayLeft, displayBottom), Offset(displayLeft, displayBottom - cornerLength), strokeWidth = cornerStroke)

                                // BR
                                drawLine(selectionColor, Offset(displayRight, displayBottom), Offset(displayRight - cornerLength, displayBottom), strokeWidth = cornerStroke)
                                drawLine(selectionColor, Offset(displayRight, displayBottom), Offset(displayRight, displayBottom - cornerLength), strokeWidth = cornerStroke)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(SumiInk)
        ) {
            WorkspaceArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            Surface(
                color = SumiInk,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .statusBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.camera_crop_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        SegmentedControl(
                            selected = interactionMode,
                            onSelected = { interactionMode = it }
                        )
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onCancel() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.camera_cancel_desc), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.camera_recapture_btn), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Button(
                            onClick = {
                                if (interactionMode == CropInteraction.TEXT_SELECT) {
                                    if (detectedBoxes.isNotEmpty()) {
                                        val startHandle = textSelectStart
                                        val endHandle = textSelectEnd
                                        if (startHandle != null && endHandle != null) {
                                            val (actualStart, actualEnd) = getOrderedHandles(startHandle, endHandle)
                                            val slicedBoxes = calculateSubLineRects(detectedBoxes, actualStart, actualEnd)
                                            try {
                                                val masked = MaskedCropHelper.createMaskedBitmap(bitmap, slicedBoxes)
                                                onConfirm(masked)
                                            } catch (e: Exception) {
                                                AppLogger.e("CAMERA", "Masked crop failed", e)
                                                onConfirm(bitmap)
                                            }
                                        } else {
                                            onConfirm(bitmap)
                                        }
                                    } else {
                                        onConfirm(bitmap)
                                    }
                                } else {
                                    val bmpW = bitmap.width
                                    val bmpH = bitmap.height
                                    
                                    val x = ((cropState.cropLeft - cropState.imgOffsetX) / cropState.scaleFactor).toInt().coerceIn(0, bmpW)
                                    val y = ((cropState.cropTop - cropState.imgOffsetY) / cropState.scaleFactor).toInt().coerceIn(0, bmpH)
                                    
                                    var w = ((cropState.cropRight - cropState.cropLeft) / cropState.scaleFactor).toInt()
                                    var h = ((cropState.cropBottom - cropState.cropTop) / cropState.scaleFactor).toInt()
                                    
                                    if (x + w > bmpW) w = bmpW - x
                                    if (y + h > bmpH) h = bmpH - y
                                    
                                    if (w > 0 && h > 0) {
                                        try {
                                            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                                            onConfirm(cropped)
                                        } catch (e: Throwable) {
                                            AppLogger.e("CAMERA", "Failed to crop manual selection", e)
                                            onConfirm(bitmap)
                                        }
                                    } else {
                                        onConfirm(bitmap)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KuriAmber,
                                contentColor = SumiInk
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.camera_confirm_desc), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.camera_confirm_btn), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SumiInk)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.camera_crop_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                SegmentedControl(
                    selected = interactionMode,
                    onSelected = { interactionMode = it },
                    modifier = Modifier.weight(2f)
                )
            }
            
            WorkspaceArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            
            Surface(
                color = SumiInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onCancel() },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.camera_cancel_desc), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.camera_recapture_btn), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    
                    Button(
                        onClick = {
                            if (interactionMode == CropInteraction.TEXT_SELECT) {
                                if (detectedBoxes.isNotEmpty()) {
                                    val startHandle = textSelectStart ?: TextHandleState(0, 0f)
                                    val endHandle = textSelectEnd ?: TextHandleState(detectedBoxes.lastIndex, 1f)
                                    val (actualStart, actualEnd) = getOrderedHandles(startHandle, endHandle)
                                    val slicedBoxes = calculateSubLineRects(detectedBoxes, actualStart, actualEnd)
                                    try {
                                        val masked = MaskedCropHelper.createMaskedBitmap(bitmap, slicedBoxes)
                                        onConfirm(masked)
                                    } catch (e: Exception) {
                                        AppLogger.e("CAMERA", "Masked crop failed", e)
                                        onConfirm(bitmap)
                                    }
                                } else {
                                    onConfirm(bitmap)
                                }
                            } else {
                                val bmpW = bitmap.width
                                val bmpH = bitmap.height
                                
                                val x = ((cropState.cropLeft - cropState.imgOffsetX) / cropState.scaleFactor).toInt().coerceIn(0, bmpW)
                                val y = ((cropState.cropTop - cropState.imgOffsetY) / cropState.scaleFactor).toInt().coerceIn(0, bmpH)
                                
                                var w = ((cropState.cropRight - cropState.cropLeft) / cropState.scaleFactor).toInt()
                                var h = ((cropState.cropBottom - cropState.cropTop) / cropState.scaleFactor).toInt()
                                
                                if (x + w > bmpW) w = bmpW - x
                                if (y + h > bmpH) h = bmpH - y
                                
                                if (w > 0 && h > 0) {
                                    try {
                                        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                                        onConfirm(cropped)
                                    } catch (e: Throwable) {
                                        AppLogger.e("CAMERA", "Failed to crop manual selection", e)
                                        onConfirm(bitmap)
                                    }
                                } else {
                                    onConfirm(bitmap)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KuriAmber,
                            contentColor = SumiInk
                        ),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.camera_confirm_desc), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.camera_confirm_btn), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}



private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

