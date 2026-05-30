package com.example.japanesegrammarapp.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.utils.BitmapHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CameraScreenMode {
    CAPTURE,
    CROP_REVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    galleryImageUriString: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core states
    var screenMode by remember { mutableStateOf(CameraScreenMode.CAPTURE) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    // CameraX helper
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
    }
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Clean up camera executor on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    // If we passed a gallery image, directly go to the crop review mode
    LaunchedEffect(galleryImageUriString) {
        if (!galleryImageUriString.isNullOrBlank()) {
            isCapturing = true
            val uri = Uri.parse(galleryImageUriString)
            val bitmap = BitmapHelper.loadRotatedBitmapFromUri(context, uri)
            if (bitmap != null) {
                capturedBitmap = bitmap
                screenMode = CameraScreenMode.CROP_REVIEW
            } else {
                // Fail and navigate back
                navController.popBackStack()
            }
            isCapturing = false
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    LaunchedEffect(hasCameraPermission, galleryImageUriString) {
        if (!hasCameraPermission && galleryImageUriString.isNullOrBlank()) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(
        containerColor = SumiInk
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = screenMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400, easing = EaseInOutCubic))
                        .togetherWith(fadeOut(animationSpec = tween(400, easing = EaseInOutCubic)))
                },
                label = "CameraScreenModeTransition",
                modifier = Modifier.fillMaxSize()
            ) { mode ->
                if (mode == CameraScreenMode.CAPTURE) {
                    // Live camera preview state
                    if (hasCameraPermission) {
                        CameraPreviewLayout(
                            imageCapture = imageCapture,
                            flashMode = flashMode,
                            onFlashToggle = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                                imageCapture.flashMode = flashMode
                            },
                            isCapturing = isCapturing,
                            onCapture = {
                                isCapturing = true
                                val file = BitmapHelper.createTempCapturedFile(context)
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                
                                val displayMetrics = context.resources.displayMetrics
                                val screenWidth = displayMetrics.widthPixels.toFloat()
                                val screenHeight = displayMetrics.heightPixels.toFloat()
                                
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            scope.launch(Dispatchers.IO) {
                                                val bitmap = BitmapHelper.loadRotatedBitmap(file)
                                                val processedBitmap = if (bitmap != null) {
                                                    cropBitmapToAspectRatio(bitmap, screenWidth, screenHeight)
                                                } else {
                                                    null
                                                }
                                                withContext(Dispatchers.Main) {
                                                    if (processedBitmap != null) {
                                                        capturedBitmap = processedBitmap
                                                        screenMode = CameraScreenMode.CROP_REVIEW
                                                    }
                                                    isCapturing = false
                                                }
                                            }
                                        }
                                        
                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraScreen", "Capture failed: ${exception.message}", exception)
                                            isCapturing = false
                                        }
                                    }
                                )
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    } else {
                        // No permission screen
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.camera_permission_required_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.camera_permission_required_desc),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = WashiBg, contentColor = SumiInk)
                            ) {
                                Text(stringResource(R.string.camera_request_permission_btn), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Interactive review and crop state
                    capturedBitmap?.let { bitmap ->
                        ImageCropReviewLayout(
                            bitmap = bitmap,
                            onCancel = {
                                if (!galleryImageUriString.isNullOrBlank()) {
                                    // If started from gallery selection, go back directly
                                    navController.popBackStack()
                                } else {
                                    // If camera capture, go back to camera preview
                                    screenMode = CameraScreenMode.CAPTURE
                                    capturedBitmap = null
                                }
                            },
                            onConfirm = { croppedBitmap ->
                                isCapturing = true
                                scope.launch(Dispatchers.IO) {
                                    val outUri = BitmapHelper.saveCroppedBitmap(context, croppedBitmap)
                                    withContext(Dispatchers.Main) {
                                        if (outUri != null) {
                                            // Set result in savedStateHandle
                                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                                "captured_image_uri",
                                                outUri.toString()
                                            )
                                        }
                                        navController.popBackStack()
                                        isCapturing = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Loading Overlay
            if (isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WashiBg)
                }
            }
        }
    }
}

@Composable
fun CameraPreviewLayout(
    imageCapture: ImageCapture,
    flashMode: Int,
    onFlashToggle: () -> Unit,
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = remember {
        Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
    }
    val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Live Preview View
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Zen Scanning Mask Overlay
        ZenScanningOverlay()
        
        // Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded translucent back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.camera_back_desc),
                    tint = Color.White
                )
            }
            
            val flashIcon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> stringResource(R.string.camera_flash_on_label)
                ImageCapture.FLASH_MODE_AUTO -> stringResource(R.string.camera_flash_auto_label)
                else -> stringResource(R.string.camera_flash_off_label)
            }
            
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onFlashToggle() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = flashIcon,
                    color = if (flashMode != ImageCapture.FLASH_MODE_OFF) KuriAmber else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Bottom Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular tactile Shutter Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(BorderStroke(4.dp, Color.White), CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !isCapturing) {
                        onCapture()
                    }
            )
        }
    }
}

@Composable
fun ZenScanningOverlay() {
    // Elegant frame and pulsing scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val laserYOffsetProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLine"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Scanning rectangle occupies 80% width, centered vertically, 45% height
        val rectW = width * 0.85f
        val rectH = height * 0.40f
        
        val left = (width - rectW) / 2
        val top = (height - rectH) / 2.3f // slightly higher than true center
        val right = left + rectW
        val bottom = top + rectH
        
        // 1. Draw dimming background around target framing box
        // Top
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, 0f), size = Size(width, top))
        // Bottom
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, bottom), size = Size(width, height - bottom))
        // Left
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, top), size = Size(left, rectH))
        // Right
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(right, top), size = Size(width - right, rectH))
        
        // 2. Draw modern minimal corner guides
        val strokeW = 4.dp.toPx()
        val cornerL = 24.dp.toPx()
        
        // Top-Left corner
        drawLine(Color.White, Offset(left - strokeW/2, top), Offset(left + cornerL, top), strokeWidth = strokeW)
        drawLine(Color.White, Offset(left, top - strokeW/2), Offset(left, top + cornerL), strokeWidth = strokeW)
        
        // Top-Right corner
        drawLine(Color.White, Offset(right + strokeW/2, top), Offset(right - cornerL, top), strokeWidth = strokeW)
        drawLine(Color.White, Offset(right, top - strokeW/2), Offset(right, top + cornerL), strokeWidth = strokeW)
        
        // Bottom-Left corner
        drawLine(Color.White, Offset(left - strokeW/2, bottom), Offset(left + cornerL, bottom), strokeWidth = strokeW)
        drawLine(Color.White, Offset(left, bottom + strokeW/2), Offset(left, bottom - cornerL), strokeWidth = strokeW)
        
        // Bottom-Right corner
        drawLine(Color.White, Offset(right + strokeW/2, bottom), Offset(right - cornerL, bottom), strokeWidth = strokeW)
        drawLine(Color.White, Offset(right, bottom + strokeW/2), Offset(right, bottom - cornerL), strokeWidth = strokeW)
        
        // 3. Draw pulsing modern "Japanese-gold" Kuri scan line inside box
        val laserY = top + (rectH * laserYOffsetProgress)
        // Soft glowing line
        drawLine(
            color = KuriAmber.copy(alpha = 0.8f),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx()
        )
        // Draw auxiliary subtle alignment lines
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(left, top + rectH / 3),
            end = Offset(right, top + rectH / 3),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(left, top + (rectH * 2) / 3),
            end = Offset(right, top + (rectH * 2) / 3),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Scanning text instruction
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 120.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_guide_keep_inside),
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.camera_guide_crop_hint),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ImageCropReviewLayout(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    
    val cropState = remember(bitmap) {
        CropState(
            bitmapWidth = bitmap.width.toFloat(),
            bitmapHeight = bitmap.height.toFloat()
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumiInk)
    ) {
        // Top instruction bar
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
                fontSize = 16.sp
            )
            
            Text(
                text = stringResource(R.string.camera_crop_hint),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        
        // Image & Cropper Workspace
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                
                val minTolerancePx = with(density) { 48.dp.toPx() }
                val minSizePx = with(density) { 60.dp.toPx() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val downEvent = awaitFirstDown(requireUnconsumed = false)
                                    var activePointerId = downEvent.id
                                    
                                    cropState.startDrag(downEvent.position, minTolerancePx)
                                    
                                    var isPinching = false
                                    var initialPinchDistance = 0f
                                    var initialLeft = cropState.cropLeft
                                    var initialTop = cropState.cropTop
                                    var initialRight = cropState.cropRight
                                    var initialBottom = cropState.cropBottom
                                    var initialCenterX = (initialLeft + initialRight) / 2f
                                    var initialCenterY = (initialTop + initialBottom) / 2f
                                    var initialWidth = initialRight - initialLeft
                                    var initialHeight = initialBottom - initialTop
                                    
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val changes = event.changes
                                        
                                        if (changes.all { !it.pressed }) {
                                            cropState.stopDrag()
                                            break
                                        }
                                        
                                        if (changes.any { it.isConsumed }) {
                                            cropState.stopDrag()
                                            break
                                        }
                                        
                                        val activeChanges = changes.filter { it.pressed }
                                        if (activeChanges.size == 1) {
                                            if (isPinching) {
                                                isPinching = false
                                                val currentFinger = activeChanges[0]
                                                activePointerId = currentFinger.id
                                                cropState.startDrag(currentFinger.position, minTolerancePx)
                                            } else {
                                                val currentFinger = activeChanges.find { it.id == activePointerId } ?: activeChanges[0]
                                                activePointerId = currentFinger.id
                                                val dragAmount = currentFinger.position - currentFinger.previousPosition
                                                currentFinger.consume()
                                                cropState.onDrag(dragAmount, minSizePx)
                                            }
                                        } else if (activeChanges.size >= 2) {
                                            val p1 = activeChanges[0]
                                            val p2 = activeChanges[1]
                                            
                                            val pos1 = p1.position
                                            val pos2 = p2.position
                                            
                                            val dx = pos1.x - pos2.x
                                            val dy = pos1.y - pos2.y
                                            val currentDistance = kotlin.math.sqrt(dx * dx + dy * dy)
                                            
                                            if (!isPinching) {
                                                isPinching = true
                                                initialPinchDistance = currentDistance
                                                initialLeft = cropState.cropLeft
                                                initialTop = cropState.cropTop
                                                initialRight = cropState.cropRight
                                                initialBottom = cropState.cropBottom
                                                initialCenterX = (initialLeft + initialRight) / 2f
                                                initialCenterY = (initialTop + initialBottom) / 2f
                                                initialWidth = initialRight - initialLeft
                                                initialHeight = initialBottom - initialTop
                                                
                                                cropState.activeHandle = DragHandle.NONE
                                            } else {
                                                if (initialPinchDistance > 5f) {
                                                    val scale = currentDistance / initialPinchDistance
                                                    
                                                    val newWidth = initialWidth * scale
                                                    val newHeight = initialHeight * scale
                                                    
                                                    val clampedWidth = maxOf(newWidth, minSizePx)
                                                    val clampedHeight = maxOf(newHeight, minSizePx)
                                                    
                                                    var newLeft = initialCenterX - clampedWidth / 2f
                                                    var newRight = initialCenterX + clampedWidth / 2f
                                                    var newTop = initialCenterY - clampedHeight / 2f
                                                    var newBottom = initialCenterY + clampedHeight / 2f
                                                    
                                                    val maxLeft = cropState.imgOffsetX
                                                    val maxRight = cropState.imgOffsetX + cropState.imgDispWidth
                                                    val maxTop = cropState.imgOffsetY
                                                    val maxBottom = cropState.imgOffsetY + cropState.imgDispHeight
                                                    
                                                    if (newLeft < maxLeft) {
                                                        val diff = maxLeft - newLeft
                                                        newLeft += diff
                                                        newRight += diff
                                                    }
                                                    if (newRight > maxRight) {
                                                        val diff = newRight - maxRight
                                                        newLeft -= diff
                                                        newRight -= diff
                                                    }
                                                    if (newTop < maxTop) {
                                                        val diff = maxTop - newTop
                                                        newTop += diff
                                                        newBottom += diff
                                                    }
                                                    if (newBottom > maxBottom) {
                                                        val diff = newBottom - maxBottom
                                                        newTop -= diff
                                                        newBottom -= diff
                                                    }
                                                    
                                                    cropState.cropLeft = maxOf(newLeft, maxLeft)
                                                    cropState.cropRight = minOf(newRight, maxRight)
                                                    cropState.cropTop = maxOf(newTop, maxTop)
                                                    cropState.cropBottom = minOf(newBottom, maxBottom)
                                                }
                                            }
                                            p1.consume()
                                            p2.consume()
                                        }
                                    }
                                }
                            }
                        }
                ) {
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
                }
            }
        }
        
        // Bottom controls row
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel/Retake Action
                OutlinedButton(
                    onClick = { onCancel() },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.camera_cancel_desc), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.camera_recapture_btn), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                
                // Confirm/Crop Action
                Button(
                    onClick = {
                        val bmpW = bitmap.width
                        val bmpH = bitmap.height
                        
                        // Map screen-space crop coordinates to actual bitmap coordinates
                        val x = ((cropState.cropLeft - cropState.imgOffsetX) / cropState.scaleFactor).toInt().coerceIn(0, bmpW)
                        val y = ((cropState.cropTop - cropState.imgOffsetY) / cropState.scaleFactor).toInt().coerceIn(0, bmpH)
                        
                        var w = ((cropState.cropRight - cropState.cropLeft) / cropState.scaleFactor).toInt()
                        var h = ((cropState.cropBottom - cropState.cropTop) / cropState.scaleFactor).toInt()
                        
                        // Coerce width and height to fit bounds safely
                        if (x + w > bmpW) w = bmpW - x
                        if (y + h > bmpH) h = bmpH - y
                        
                        if (w > 0 && h > 0) {
                            try {
                                val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                                onConfirm(cropped)
                            } catch (e: IllegalArgumentException) {
                                e.printStackTrace()
                                onConfirm(bitmap) // Fallback to raw captured bitmap if crop fails
                            }
                        } else {
                            onConfirm(bitmap) // Fallback
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuriAmber,
                        contentColor = SumiInk
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.camera_confirm_desc), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.camera_confirm_btn), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun cropBitmapToAspectRatio(bitmap: Bitmap, screenWidth: Float, screenHeight: Float): Bitmap {
    val bmpW = bitmap.width.toFloat()
    val bmpH = bitmap.height.toFloat()
    if (bmpW <= 0f || bmpH <= 0f || screenWidth <= 0f || screenHeight <= 0f) return bitmap
    
    val screenRatio = screenWidth / screenHeight
    val bmpRatio = bmpW / bmpH
    
    return try {
        if (screenRatio < bmpRatio) {
            // Screen is taller/narrower than bitmap. Crop horizontal sides.
            val targetWidth = bmpH * screenRatio
            val xOffset = ((bmpW - targetWidth) / 2f).toInt().coerceIn(0, (bmpW - 1).toInt())
            val targetWidthInt = targetWidth.toInt().coerceIn(1, (bmpW - xOffset).toInt())
            Bitmap.createBitmap(bitmap, xOffset, 0, targetWidthInt, bitmap.height)
        } else {
            // Screen is wider/shorter than bitmap. Crop vertical sides.
            val targetHeight = bmpW / screenRatio
            val yOffset = ((bmpH - targetHeight) / 2f).toInt().coerceIn(0, (bmpH - 1).toInt())
            val targetHeightInt = targetHeight.toInt().coerceIn(1, (bmpH - yOffset).toInt())
            Bitmap.createBitmap(bitmap, 0, yOffset, bitmap.width, targetHeightInt)
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to crop captured bitmap to screen aspect ratio", e)
        bitmap
    }
}

