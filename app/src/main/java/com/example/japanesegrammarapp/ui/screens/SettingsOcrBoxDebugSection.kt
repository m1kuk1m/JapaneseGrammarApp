package com.example.japanesegrammarapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectorEngine
import com.example.japanesegrammarapp.domain.model.OcrBoxPreviewMode
import com.example.japanesegrammarapp.utils.BitmapHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OcrBoxDebugDialog(
    settings: OcrBoxDetectionSettings,
    onSettingsChange: (OcrBoxDetectionSettings) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()

    var sampleBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedBoxes by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var isLoadingBitmap by remember { mutableStateOf(false) }
    var isDetecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(sampleBitmap) {
        val bitmapToDispose = sampleBitmap
        onDispose {
            bitmapToDispose?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    val sampleImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            isLoadingBitmap = true
            errorMessage = null
            sampleBitmap = null

            coroutineScope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    BitmapHelper.loadRotatedBitmapFromUri(context, uri, OCR_DEBUG_MAX_DIMENSION)
                }
                sampleBitmap = loaded
                isLoadingBitmap = false
                if (loaded == null) {
                    errorMessage = context.getString(R.string.ocr_debug_load_failed)
                }
            }
        }
    }

    LaunchedEffect(sampleBitmap, settings) {
        val bitmap = sampleBitmap ?: return@LaunchedEffect
        delay(OCR_DEBUG_DETECT_DEBOUNCE_MS)
        isDetecting = true
        errorMessage = null
        try {
            detectedBoxes = detectCameraOcrBoxes(
                bitmap = bitmap,
                settings = settings,
                context = context
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            detectedBoxes = emptyList()
            errorMessage = e.localizedMessage ?: context.getString(R.string.unknown_error)
        } finally {
            isDetecting = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ocr_debug_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = sumiInk
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = sumiInk
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = sumiInk.copy(alpha = 0.1f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { sampleImageLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(containerColor = primary)
                            ) {
                                Text(stringResource(R.string.ocr_debug_pick_sample), textAlign = TextAlign.Center)
                            }
                            OutlinedButton(onClick = onResetDefaults, modifier = Modifier.fillMaxHeight()) {
                                Text(stringResource(R.string.ocr_debug_reset_defaults), textAlign = TextAlign.Center)
                            }
                        }

                        OcrOptionRow(
                            label = stringResource(R.string.ocr_debug_detector_engine),
                            options = listOf(
                                OcrBoxDetectorEngine.ML_KIT to stringResource(R.string.ocr_debug_engine_mlkit),
                                OcrBoxDetectorEngine.RAPID_OCR to stringResource(R.string.ocr_debug_engine_rapidocr),
                                OcrBoxDetectorEngine.HYBRID to stringResource(R.string.ocr_debug_engine_hybrid),
                                OcrBoxDetectorEngine.AUTO to stringResource(R.string.ocr_debug_engine_auto)
                            ),
                            selected = settings.detectorEngine,
                            onSelected = { engine -> onSettingsChange(settings.copy(detectorEngine = engine)) }
                        )

                        OcrOptionRow(
                            label = stringResource(R.string.ocr_debug_preview_mode),
                            options = listOf(
                                OcrBoxPreviewMode.FINAL to stringResource(R.string.ocr_debug_preview_final),
                                OcrBoxPreviewMode.RAW to stringResource(R.string.ocr_debug_preview_raw)
                            ),
                            selected = settings.previewMode,
                            onSelected = { mode -> onSettingsChange(settings.copy(previewMode = mode)) }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(sumiInk.copy(alpha = 0.05f))
                                .border(1.dp, sumiInk.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = sampleBitmap
                            when {
                                isLoadingBitmap -> CircularProgressIndicator()
                                bitmap == null -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.ImageSearch,
                                            contentDescription = null,
                                            tint = sumiInk.copy(alpha = 0.35f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.ocr_debug_empty_preview),
                                            color = sumiInk.copy(alpha = 0.55f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                else -> OcrDebugPreview(
                                    bitmap = bitmap,
                                    boxes = detectedBoxes,
                                    isDetecting = isDetecting
                                )
                            }
                        }

                        Text(
                            text = if (sampleBitmap == null) {
                                stringResource(R.string.ocr_debug_preview_hint)
                            } else {
                                stringResource(R.string.ocr_debug_box_count, detectedBoxes.size)
                            },
                            color = sumiInk.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )

                        errorMessage?.let { message ->
                            Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Text(
                            text = stringResource(R.string.ocr_debug_rapidocr_settings),
                            color = sumiInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_rapidocr_input_size),
                            value = settings.rapidOcrInputLongSide.toFloat(),
                            valueRange = OcrBoxDetectionSettings.RAPID_OCR_INPUT_LONG_SIDE_MIN.toFloat()..OcrBoxDetectionSettings.RAPID_OCR_INPUT_LONG_SIDE_MAX.toFloat(),
                            onValueChange = { onSettingsChange(settings.copy(rapidOcrInputLongSide = it.toInt())) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_rapidocr_det_threshold),
                            value = settings.rapidOcrDetThreshold,
                            valueRange = OcrBoxDetectionSettings.THRESHOLD_MIN..OcrBoxDetectionSettings.THRESHOLD_MAX,
                            onValueChange = { onSettingsChange(settings.copy(rapidOcrDetThreshold = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_rapidocr_box_threshold),
                            value = settings.rapidOcrBoxThreshold,
                            valueRange = OcrBoxDetectionSettings.THRESHOLD_MIN..OcrBoxDetectionSettings.THRESHOLD_MAX,
                            onValueChange = { onSettingsChange(settings.copy(rapidOcrBoxThreshold = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_rapidocr_unclip),
                            value = settings.rapidOcrUnclipRatio,
                            valueRange = OcrBoxDetectionSettings.RAPID_OCR_UNCLIP_MIN..OcrBoxDetectionSettings.RAPID_OCR_UNCLIP_MAX,
                            onValueChange = { onSettingsChange(settings.copy(rapidOcrUnclipRatio = it)) }
                        )

                        Text(
                            text = stringResource(R.string.ocr_debug_merge_settings),
                            color = sumiInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_horizontal_gap),
                            value = settings.horizontalRowGapMultiplier,
                            valueRange = OcrBoxDetectionSettings.MERGE_GAP_MIN..OcrBoxDetectionSettings.MERGE_GAP_MAX,
                            onValueChange = { onSettingsChange(settings.copy(horizontalRowGapMultiplier = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_horizontal_overlap),
                            value = settings.horizontalXOverlapThreshold,
                            valueRange = OcrBoxDetectionSettings.THRESHOLD_MIN..OcrBoxDetectionSettings.THRESHOLD_MAX,
                            onValueChange = { onSettingsChange(settings.copy(horizontalXOverlapThreshold = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_horizontal_fill),
                            value = settings.horizontalFillRatioMin,
                            valueRange = OcrBoxDetectionSettings.THRESHOLD_MIN..OcrBoxDetectionSettings.FILL_RATIO_MAX,
                            onValueChange = { onSettingsChange(settings.copy(horizontalFillRatioMin = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_vertical_gap),
                            value = settings.verticalColumnGapMultiplier,
                            valueRange = OcrBoxDetectionSettings.MERGE_GAP_MIN..OcrBoxDetectionSettings.MERGE_GAP_MAX,
                            onValueChange = { onSettingsChange(settings.copy(verticalColumnGapMultiplier = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_vertical_overlap),
                            value = settings.verticalXOverlapThreshold,
                            valueRange = OcrBoxDetectionSettings.THRESHOLD_MIN..OcrBoxDetectionSettings.THRESHOLD_MAX,
                            onValueChange = { onSettingsChange(settings.copy(verticalXOverlapThreshold = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_vertical_fill),
                            value = settings.verticalFillRatioMin,
                            valueRange = OcrBoxDetectionSettings.THRESHOLD_MIN..OcrBoxDetectionSettings.FILL_RATIO_MAX,
                            onValueChange = { onSettingsChange(settings.copy(verticalFillRatioMin = it)) }
                        )

                        Text(
                            text = stringResource(R.string.ocr_debug_padding_settings),
                            color = sumiInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_horizontal_padding_x),
                            value = settings.horizontalPaddingXRatio,
                            valueRange = OcrBoxDetectionSettings.PADDING_MIN..OcrBoxDetectionSettings.PADDING_MAX,
                            onValueChange = { onSettingsChange(settings.copy(horizontalPaddingXRatio = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_horizontal_padding_y),
                            value = settings.horizontalPaddingYRatio,
                            valueRange = OcrBoxDetectionSettings.PADDING_MIN..OcrBoxDetectionSettings.PADDING_MAX,
                            onValueChange = { onSettingsChange(settings.copy(horizontalPaddingYRatio = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_vertical_padding_x),
                            value = settings.verticalPaddingXRatio,
                            valueRange = OcrBoxDetectionSettings.PADDING_MIN..OcrBoxDetectionSettings.PADDING_MAX,
                            onValueChange = { onSettingsChange(settings.copy(verticalPaddingXRatio = it)) }
                        )
                        OcrSettingSlider(
                            label = stringResource(R.string.ocr_debug_vertical_padding_y),
                            value = settings.verticalPaddingYRatio,
                            valueRange = OcrBoxDetectionSettings.PADDING_MIN..OcrBoxDetectionSettings.PADDING_MAX,
                            onValueChange = { onSettingsChange(settings.copy(verticalPaddingYRatio = it)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrSettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = sumiInk, fontSize = 13.sp)
            Text(String.format(java.util.Locale.US, "%.2f", value), color = sumiInk.copy(alpha = 0.55f), fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 0
        )
    }
}

@Composable
private fun <T> OcrOptionRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = sumiInk, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, text) ->
                val isSelected = value == selected
                val contentColor = if (isSelected) Color.White else sumiInk
                TextButton(
                    onClick = { onSelected(value) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) primary else sumiInk.copy(alpha = 0.06f),
                        contentColor = contentColor
                    )
                ) {
                    Text(text = text, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun OcrDebugPreview(
    bitmap: Bitmap,
    boxes: List<Rect>,
    isDetecting: Boolean
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
            val displayWidth = bitmap.width * scale
            val displayHeight = bitmap.height * scale
            val offsetX = (size.width - displayWidth) / 2f
            val offsetY = (size.height - displayHeight) / 2f

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(displayWidth.toInt(), displayHeight.toInt())
            )

            boxes.forEach { box ->
                val left = offsetX + box.left * scale
                val top = offsetY + box.top * scale
                val width = box.width() * scale
                val height = box.height() * scale
                drawRect(
                    color = Color(0xFFFFD166),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawRect(
                    color = Color(0xFFFFD166).copy(alpha = 0.16f),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
            }
        }

        if (isDetecting) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

private const val OCR_DEBUG_MAX_DIMENSION = 1600
private const val OCR_DEBUG_DETECT_DEBOUNCE_MS = 450L
