package com.example.japanesegrammarapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CameraScreenMode {
    CAPTURE,
    CROP_REVIEW
}

enum class DragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    galleryImageUriString: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var ocrTargetMode by remember { mutableStateOf(true) } // Guides target overlay
    
    // CameraX helper
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
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
            val bitmap = loadRotatedBitmapFromUri(context, uri)
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
                            cameraExecutor = cameraExecutor,
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
                                val imagesDir = File(context.filesDir, "images")
                                if (!imagesDir.exists()) imagesDir.mkdirs()
                                val file = File(imagesDir, "temp_captured_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            scope.launch(Dispatchers.IO) {
                                                val bitmap = loadRotatedBitmap(file)
                                                withContext(Dispatchers.Main) {
                                                    if (bitmap != null) {
                                                        capturedBitmap = bitmap
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
                                    val imagesDir = File(context.filesDir, "images")
                                    if (!imagesDir.exists()) imagesDir.mkdirs()
                                    val outFile = File(imagesDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(outFile).use { out ->
                                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                    }
                                    val outUri = Uri.fromFile(outFile)
                                    withContext(Dispatchers.Main) {
                                        // Set result in savedStateHandle
                                        navController.previousBackStackEntry?.savedStateHandle?.set(
                                            "captured_image_uri",
                                            outUri.toString()
                                        )
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
    cameraExecutor: ExecutorService,
    flashMode: Int,
    onFlashToggle: () -> Unit,
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = remember { Preview.Builder().build() }
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
            
            // Rounded translucent flash pill toggle
            val flashLabel = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> stringResource(R.string.camera_flash_on_desc)
                ImageCapture.FLASH_MODE_AUTO -> stringResource(R.string.camera_flash_auto_desc)
                else -> stringResource(R.string.camera_flash_off_desc)
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
    val context = LocalContext.current
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }
    
    // Crop rectangle coordinates in screen pixels
    var cropLeft by remember { mutableStateOf(0f) }
    var cropTop by remember { mutableStateOf(0f) }
    var cropRight by remember { mutableStateOf(0f) }
    var cropBottom by remember { mutableStateOf(0f) }
    
    var isInitialized by remember { mutableStateOf(false) }
    
    // Fit image dimension inside view
    var imgDispWidth by remember { mutableStateOf(0f) }
    var imgDispHeight by remember { mutableStateOf(0f) }
    var scaleFactor by remember { mutableStateOf(1f) }
    var imgOffsetX by remember { mutableStateOf(0f) }
    var imgOffsetY by remember { mutableStateOf(0f) }
    
    // Track active touched handle during drag
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    
    // Compute drawing dimensions when container size is resolved
    fun initializeCropBox() {
        if (containerWidth <= 0f || containerHeight <= 0f || isInitialized) return
        
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        
        // ContentScale.Fit logic
        val scaleX = containerWidth / bmpW
        val scaleY = containerHeight / bmpH
        scaleFactor = minOf(scaleX, scaleY)
        
        imgDispWidth = bmpW * scaleFactor
        imgDispHeight = bmpH * scaleFactor
        
        imgOffsetX = (containerWidth - imgDispWidth) / 2
        imgOffsetY = (containerHeight - imgDispHeight) / 2
        
        // Initial crop box at center (covers 80% width and 40% height of displayed image)
        val initialW = imgDispWidth * 0.8f
        val initialH = imgDispHeight * 0.4f
        
        cropLeft = imgOffsetX + (imgDispWidth - initialW) / 2
        cropTop = imgOffsetY + (imgDispHeight - initialH) / 2
        cropRight = cropLeft + initialW
        cropBottom = cropTop + initialH
        
        isInitialized = true
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
                    containerWidth = layoutCoordinates.size.width.toFloat()
                    containerHeight = layoutCoordinates.size.height.toFloat()
                    initializeCropBox()
                }
        ) {
            if (isInitialized) {
                // Renders the original full-size image scaled to fit
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw original scaled image
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(imgOffsetX.toInt(), imgOffsetY.toInt()),
                        dstSize = IntSize(imgDispWidth.toInt(), imgDispHeight.toInt())
                    )
                }
                
                // Draggable Crop overlay
                val density = LocalDensity.current
                val handleRadiusPx = with(density) { 14.dp.toPx() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val x = offset.x
                                    val y = offset.y
                                    
                                    val distTL = distance(x, y, cropLeft, cropTop)
                                    val distTR = distance(x, y, cropRight, cropTop)
                                    val distBL = distance(x, y, cropLeft, cropBottom)
                                    val distBR = distance(x, y, cropRight, cropBottom)
                                    
                                    val minTolerance = 48.dp.toPx()
                                    
                                    activeHandle = when {
                                        distTL < minTolerance -> DragHandle.TOP_LEFT
                                        distTR < minTolerance -> DragHandle.TOP_RIGHT
                                        distBL < minTolerance -> DragHandle.BOTTOM_LEFT
                                        distBR < minTolerance -> DragHandle.BOTTOM_RIGHT
                                        x in cropLeft..cropRight && y in cropTop..cropBottom -> DragHandle.CENTER
                                        else -> DragHandle.NONE
                                    }
                                },
                                onDragEnd = {
                                    activeHandle = DragHandle.NONE
                                },
                                onDragCancel = {
                                    activeHandle = DragHandle.NONE
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    
                                    val dx = dragAmount.x
                                    val dy = dragAmount.y
                                    
                                    val minSize = 60.dp.toPx()
                                    
                                    when (activeHandle) {
                                        DragHandle.CENTER -> {
                                            val w = cropRight - cropLeft
                                            val h = cropBottom - cropTop
                                            
                                            cropLeft = (cropLeft + dx).coerceIn(imgOffsetX, imgOffsetX + imgDispWidth - w)
                                            cropRight = cropLeft + w
                                            cropTop = (cropTop + dy).coerceIn(imgOffsetY, imgOffsetY + imgDispHeight - h)
                                            cropBottom = cropTop + h
                                        }
                                        DragHandle.TOP_LEFT -> {
                                            cropLeft = (cropLeft + dx).coerceIn(imgOffsetX, cropRight - minSize)
                                            cropTop = (cropTop + dy).coerceIn(imgOffsetY, cropBottom - minSize)
                                        }
                                        DragHandle.TOP_RIGHT -> {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, imgOffsetX + imgDispWidth)
                                            cropTop = (cropTop + dy).coerceIn(imgOffsetY, cropBottom - minSize)
                                        }
                                        DragHandle.BOTTOM_LEFT -> {
                                            cropLeft = (cropLeft + dx).coerceIn(imgOffsetX, cropRight - minSize)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, imgOffsetY + imgDispHeight)
                                        }
                                        DragHandle.BOTTOM_RIGHT -> {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, imgOffsetX + imgDispWidth)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, imgOffsetY + imgDispHeight)
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }
                ) {
                    // Draw dimming mask outside crop box bounds
                    // Top mask
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(imgOffsetX, imgOffsetY),
                        size = Size(imgDispWidth, cropTop - imgOffsetY)
                    )
                    // Bottom mask
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(imgOffsetX, cropBottom),
                        size = Size(imgDispWidth, imgOffsetY + imgDispHeight - cropBottom)
                    )
                    // Left mask
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(imgOffsetX, cropTop),
                        size = Size(cropLeft - imgOffsetX, cropBottom - cropTop)
                    )
                    // Right mask
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(cropRight, cropTop),
                        size = Size(imgOffsetX + imgDispWidth - cropRight, cropBottom - cropTop)
                    )
                    
                    // Draw elegant crop frame borders
                    val borderStrokeW = 2.dp.toPx()
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cropLeft, cropTop),
                        size = Size(cropRight - cropLeft, cropBottom - cropTop),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderStrokeW)
                    )
                    
                    // Auxiliary grid lines (Rule of Thirds style inside crop frame)
                    val frameW = cropRight - cropLeft
                    val frameH = cropBottom - cropTop
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(cropLeft + frameW / 3f, cropTop),
                        end = Offset(cropLeft + frameW / 3f, cropBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(cropLeft + (frameW * 2f) / 3f, cropTop),
                        end = Offset(cropLeft + (frameW * 2f) / 3f, cropBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(cropLeft, cropTop + frameH / 3f),
                        end = Offset(cropRight, cropTop + frameH / 3f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(cropLeft, cropTop + (frameH * 2f) / 3f),
                        end = Offset(cropRight, cropTop + (frameH * 2f) / 3f),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Draw 4 handles (Japanese-inspired accent circles)
                    val handleRadius = 7.dp.toPx()
                    val activeColor = KuriAmber
                    val defaultColor = Color.White
                    
                    // TL
                    drawCircle(
                        color = if (activeHandle == DragHandle.TOP_LEFT) activeColor else defaultColor,
                        radius = handleRadius,
                        center = Offset(cropLeft, cropTop)
                    )
                    // TR
                    drawCircle(
                        color = if (activeHandle == DragHandle.TOP_RIGHT) activeColor else defaultColor,
                        radius = handleRadius,
                        center = Offset(cropRight, cropTop)
                    )
                    // BL
                    drawCircle(
                        color = if (activeHandle == DragHandle.BOTTOM_LEFT) activeColor else defaultColor,
                        radius = handleRadius,
                        center = Offset(cropLeft, cropBottom)
                    )
                    // BR
                    drawCircle(
                        color = if (activeHandle == DragHandle.BOTTOM_RIGHT) activeColor else defaultColor,
                        radius = handleRadius,
                        center = Offset(cropRight, cropBottom)
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
                        val x = ((cropLeft - imgOffsetX) / scaleFactor).toInt().coerceIn(0, bmpW)
                        val y = ((cropTop - imgOffsetY) / scaleFactor).toInt().coerceIn(0, bmpH)
                        
                        var w = ((cropRight - cropLeft) / scaleFactor).toInt()
                        var h = ((cropBottom - cropTop) / scaleFactor).toInt()
                        
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

// Distance helper
private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

// Loads a photo from file and corrects rotation according to EXIF
private fun loadRotatedBitmap(file: File): Bitmap? {
    try {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        Log.e("CameraScreen", "Error loading rotated bitmap", e)
        return null
    }
}

// Load rotated bitmap from ContentProvider Uri (Gallery selection)
private fun loadRotatedBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            // Check rotation using EXIF
            var rotation = 0
            context.contentResolver.openInputStream(uri)?.use { exifInputStream ->
                try {
                    val exif = ExifInterface(exifInputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotation = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Error reading EXIF from uri input stream", e)
                }
            }
            
            if (rotation == 0) return bitmap
            
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Error loading bitmap from URI", e)
    }
    return null
}
