package com.example.japanesegrammarapp.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import android.view.OrientationEventListener
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
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.SettingsViewModel
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import com.example.japanesegrammarapp.utils.BitmapHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

enum class DeviceOrientation(val rotationDegrees: Float, val targetRotation: Int) {
    PORTRAIT(0f, Surface.ROTATION_0),
    LANDSCAPE_LEFT(270f, Surface.ROTATION_90),    // charging port is on the right
    LANDSCAPE_RIGHT(90f, Surface.ROTATION_270),  // charging port is on the left
    INVERTED_PORTRAIT(180f, Surface.ROTATION_180)
}

@Composable
fun rememberDeviceOrientation(): DeviceOrientation {
    val context = LocalContext.current
    var orientation by remember { mutableStateOf(DeviceOrientation.PORTRAIT) }
    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val newOrientation = when (degrees) {
                    in 45 until 135 -> DeviceOrientation.LANDSCAPE_RIGHT
                    in 135 until 225 -> DeviceOrientation.INVERTED_PORTRAIT
                    in 225 until 315 -> DeviceOrientation.LANDSCAPE_LEFT
                    else -> DeviceOrientation.PORTRAIT
                }
                if (newOrientation != orientation) {
                    orientation = newOrientation
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose {
            listener.disable()
        }
    }
    return orientation
}

enum class CameraScreenMode {
    CAPTURE,
    CROP_REVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    galleryImageUriString: String? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val activity = context as? Activity
    DisposableEffect(activity) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val restoreOrientation = if (originalOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            originalOrientation
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = restoreOrientation
        }
    }
    
    // Core states
    val deviceOrientation = rememberDeviceOrientation()
    var screenMode by rememberSaveable { mutableStateOf(CameraScreenMode.CAPTURE) }
    var flashMode by rememberSaveable { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var tempFileUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    fun replaceCapturedBitmap(bitmap: Bitmap?) {
        val oldBitmap = capturedBitmap
        if (oldBitmap != null && oldBitmap !== bitmap && !oldBitmap.isRecycled) {
            oldBitmap.recycle()
        }
        capturedBitmap = bitmap
    }
    
    // CameraX helper
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
    }
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Clean up camera and large bitmap resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            capturedBitmap?.takeIf { !it.isRecycled }?.recycle()
            cameraExecutor.shutdownNow()
        }
    }
    
    // If we passed a gallery image, directly go to the crop review mode
    LaunchedEffect(galleryImageUriString) {
        if (!galleryImageUriString.isNullOrBlank()) {
            isCapturing = true
            val uri = Uri.parse(galleryImageUriString)
            val bitmap = BitmapHelper.loadRotatedBitmapFromUri(context, uri)
            if (bitmap != null) {
                replaceCapturedBitmap(bitmap)
                screenMode = CameraScreenMode.CROP_REVIEW
            } else {
                // Fail and navigate back
                navController.popBackStack()
            }
            isCapturing = false
        }
    }

    // Restore captured image if saved state contains a path
    LaunchedEffect(tempFileUriString) {
        if (!tempFileUriString.isNullOrBlank() && capturedBitmap == null) {
            isCapturing = true
            val uri = Uri.parse(tempFileUriString)
            val bitmap = BitmapHelper.loadRotatedBitmapFromUri(context, uri)
            if (bitmap != null) {
                replaceCapturedBitmap(bitmap)
                screenMode = CameraScreenMode.CROP_REVIEW
            }
            isCapturing = false
        }
    }
    
    var hasRequestedPermission by remember { mutableStateOf(false) }
    
    val showGoToSettings = remember(hasCameraPermission, hasRequestedPermission) {
        if (!hasCameraPermission && hasRequestedPermission) {
            val act = context as? Activity
            act != null && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(act, Manifest.permission.CAMERA)
        } else {
            false
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        hasRequestedPermission = true
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
                                
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            scope.launch(Dispatchers.IO) {
                                                // Load the bitmap with EXIF rotation applied,
                                                // but do NOT pre-crop to screen aspect ratio.
                                                // The ImageCropReviewLayout handles any image shape
                                                // correctly, and pre-cropping here causes display
                                                // errors when the user rotates back to portrait after
                                                // a landscape capture.
                                                val bitmap = BitmapHelper.loadRotatedBitmap(file)
                                                if (bitmap != null) {
                                                    val savedUri = BitmapHelper.saveCroppedBitmap(context, bitmap)
                                                    withContext(Dispatchers.Main) {
                                                        replaceCapturedBitmap(bitmap)
                                                        tempFileUriString = savedUri?.toString()
                                                        screenMode = CameraScreenMode.CROP_REVIEW
                                                        isCapturing = false
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        isCapturing = false
                                                    }
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
                            },
                            deviceOrientation = deviceOrientation
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
                                text = stringResource(
                                    if (showGoToSettings) R.string.camera_permission_denied_permanently_title
                                    else R.string.camera_permission_required_title
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    if (showGoToSettings) R.string.camera_permission_denied_permanently_desc
                                    else R.string.camera_permission_required_desc
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (showGoToSettings) {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WashiBg, contentColor = SumiInk)
                            ) {
                                Text(
                                    text = stringResource(
                                        if (showGoToSettings) R.string.camera_go_to_settings_btn
                                        else R.string.camera_request_permission_btn
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                            ) {
                                Text(stringResource(R.string.back), fontWeight = FontWeight.SemiBold)
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
                                    replaceCapturedBitmap(null)
                                    tempFileUriString = null
                                }
                            },
                            onConfirm = { croppedBitmap ->
                                isCapturing = true
                                val sourceBitmap = capturedBitmap
                                scope.launch(Dispatchers.IO) {
                                    val outUri = BitmapHelper.saveCroppedBitmap(context, croppedBitmap)
                                    if (croppedBitmap !== sourceBitmap && !croppedBitmap.isRecycled) {
                                        croppedBitmap.recycle()
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (outUri != null) {
                                            // Set result in savedStateHandle
                                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                                "captured_image_uri",
                                                outUri.toString()
                                            )
                                        }
                                        replaceCapturedBitmap(null)
                                        tempFileUriString = null
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
    onBack: () -> Unit,
    deviceOrientation: DeviceOrientation
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = remember {
        Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
    }
    val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to unbind camera on dispose", e)
            }
        }
    }

    // Update CameraX target rotation whenever device physical orientation changes
    LaunchedEffect(deviceOrientation) {
        try {
            imageCapture.targetRotation = deviceOrientation.targetRotation
            preview.targetRotation = deviceOrientation.targetRotation
        } catch (e: Exception) {
            Log.e("CameraScreen", "Failed to update target rotation dynamically", e)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

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

        // Rotation angle for UI icons (Back, Flash)
        val targetRotationDegrees = when (deviceOrientation) {
            DeviceOrientation.PORTRAIT -> 0f
            DeviceOrientation.LANDSCAPE_LEFT -> 90f
            DeviceOrientation.LANDSCAPE_RIGHT -> -90f
            DeviceOrientation.INVERTED_PORTRAIT -> 180f
        }
        val animatedRotationDegrees by animateFloatAsState(
            targetValue = targetRotationDegrees,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "uiRotation"
        )

        // Dynamic Position Animations for Shutter Button
        val shutterSize = 76.dp
        val targetShutterX = (maxWidth - shutterSize) / 2
        val targetShutterY = maxHeight - shutterSize - 32.dp

        val animatedShutterX by animateDpAsState(
            targetValue = targetShutterX,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "shutterX"
        )
        val animatedShutterY by animateDpAsState(
            targetValue = targetShutterY,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "shutterY"
        )

        // Guide text box size and coordinate animations
        val textBoxWidth = 280.dp
        val textBoxHeight = 80.dp
        val halfW = textBoxWidth / 2
        val halfH = textBoxHeight / 2

        val targetTextCenterX = when (deviceOrientation) {
            DeviceOrientation.PORTRAIT -> maxWidth / 2
            DeviceOrientation.LANDSCAPE_LEFT -> 32.dp + halfH
            DeviceOrientation.LANDSCAPE_RIGHT -> maxWidth - 32.dp - halfH
            DeviceOrientation.INVERTED_PORTRAIT -> maxWidth / 2
        }

        val targetTextCenterY = when (deviceOrientation) {
            DeviceOrientation.PORTRAIT -> (maxHeight - shutterSize - 32.dp) - 16.dp - halfH
            DeviceOrientation.LANDSCAPE_LEFT -> maxHeight / 2
            DeviceOrientation.LANDSCAPE_RIGHT -> maxHeight / 2
            DeviceOrientation.INVERTED_PORTRAIT -> 32.dp + halfH
        }

        val animatedTextX by animateDpAsState(
            targetValue = targetTextCenterX - halfW,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "textX"
        )
        val animatedTextY by animateDpAsState(
            targetValue = targetTextCenterY - halfH,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "textY"
        )

        // Back button – top-left
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = animatedRotationDegrees }
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
        }

        // Flash button – top-right
        val (flashVectorIcon, flashTextRes, flashDescRes) = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> Triple(Icons.Rounded.FlashOn, R.string.camera_flash_on_label, R.string.camera_flash_on_desc)
            ImageCapture.FLASH_MODE_AUTO -> Triple(Icons.Rounded.FlashAuto, R.string.camera_flash_auto_label, R.string.camera_flash_auto_desc)
            else -> Triple(Icons.Rounded.FlashOff, R.string.camera_flash_off_label, R.string.camera_flash_off_desc)
        }
        val labelText = stringResource(flashTextRes).replace("⚡", "").trim()

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(end = 16.dp, top = 12.dp)
                .align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = animatedRotationDegrees }
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onFlashToggle() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = flashVectorIcon,
                        contentDescription = stringResource(flashDescRes),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = labelText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated tactile Shutter Button
        Box(
            modifier = Modifier
                .absoluteOffset { IntOffset(animatedShutterX.roundToPx(), animatedShutterY.roundToPx()) }
                .size(shutterSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(BorderStroke(4.dp, Color.White), CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !isCapturing) { onCapture() }
            )
        }

        // Scanning Guide Text Instruction
        Box(
            modifier = Modifier
                .absoluteOffset { IntOffset(animatedTextX.roundToPx(), animatedTextY.roundToPx()) }
                .size(width = textBoxWidth, height = textBoxHeight),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = animatedRotationDegrees }
            ) {
                Text(
                    text = stringResource(R.string.camera_guide_keep_inside),
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.camera_guide_crop_hint),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ZenScanningOverlay() {
    // Elegant frame and pulsing scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val laserYOffsetProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLine"
    )

    val targetLeft = 16.dp
    val targetRightPadding = 16.dp
    val targetTop = 80.dp
    val targetBottomPadding = 140.dp

    val animatedLeft by animateDpAsState(targetLeft, label = "overlayLeft")
    val animatedRightPadding by animateDpAsState(targetRightPadding, label = "overlayRight")
    val animatedTop by animateDpAsState(targetTop, label = "overlayTop")
    val animatedBottomPadding by animateDpAsState(targetBottomPadding, label = "overlayBottom")
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val left = animatedLeft.toPx()
        val right = width - animatedRightPadding.toPx()
        val top = animatedTop.toPx()
        val bottom = height - animatedBottomPadding.toPx()
        
        // 1. Draw modern minimal corner guides
        val strokeW = 2.dp.toPx()
        val cornerL = 20.dp.toPx()
        val cornerColor = Color.White.copy(alpha = 0.35f)
        
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerL, top), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerL), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerL, top), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerL), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerL, bottom), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerL), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerL, bottom), strokeWidth = strokeW)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerL), strokeWidth = strokeW)
        
        // 2. Pulsing "Japanese-gold" Kuri scan line
        val laserY = top + ((bottom - top) * laserYOffsetProgress)
        drawLine(
            color = KuriAmber.copy(alpha = 0.7f),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun ImageCropReviewLayout(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val cropState = remember(bitmap) {
        CropState(
            bitmapWidth = bitmap.width.toFloat(),
            bitmapHeight = bitmap.height.toFloat()
        )
    }

    // When the screen orientation changes, the container size changes too.
    // Reset isInitialized so the crop box is recalculated for the new layout.
    LaunchedEffect(isLandscape) {
        cropState.isInitialized = false
    }

    var detectedBoxes by remember(bitmap) { mutableStateOf<List<Rect>>(emptyList()) }
    var hideOcrBoxes by remember { mutableStateOf(false) }

    LaunchedEffect(bitmap) {
        try {
            val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = mutableListOf<Rect>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            line.boundingBox?.let { lines.add(it) }
                        }
                    }

                    val clusters = mutableListOf<MutableList<Rect>>()
                    for (line in lines) {
                        clusters.add(mutableListOf(line))
                    }

                    var changed = true
                    while (changed) {
                        changed = false
                        for (i in clusters.indices) {
                            for (j in i + 1 until clusters.size) {
                                val isClose = clusters[i].any { lineI ->
                                    clusters[j].any { lineJ ->
                                        val sizeI = minOf(lineI.width(), lineI.height())
                                        val sizeJ = minOf(lineJ.width(), lineJ.height())
                                        
                                        // Refined clustering: Only merge if they are very close AND roughly on the same axis
                                        // 1. Distance threshold is tightened to 0.8x of font size
                                        val maxDist = maxOf(sizeI, sizeJ) * 0.8f
                                        
                                        // 2. Size difference shouldn't be too drastic (prevent merging title with small text)
                                        val isSizeSimilar = maxOf(sizeI, sizeJ).toFloat() / minOf(sizeI, sizeJ).toFloat() < 2.5f
                                        
                                        // 3. Directional check (must overlap significantly on either X or Y axis)
                                        val overlapX = maxOf(0, minOf(lineI.right, lineJ.right) - maxOf(lineI.left, lineJ.left))
                                        val overlapY = maxOf(0, minOf(lineI.bottom, lineJ.bottom) - maxOf(lineI.top, lineJ.top))
                                        
                                        val isAligned = overlapX > 0 || overlapY > 0
                                        
                                        if (!isSizeSimilar || !isAligned) {
                                            false
                                        } else {
                                            val expandedLine = Rect(
                                                (lineI.left - maxDist).toInt(), (lineI.top - maxDist).toInt(),
                                                (lineI.right + maxDist).toInt(), (lineI.bottom + maxDist).toInt()
                                            )
                                            Rect.intersects(expandedLine, lineJ)
                                        }
                                    }
                                }
                                if (isClose) {
                                    clusters[i].addAll(clusters[j])
                                    clusters.removeAt(j)
                                    changed = true
                                    break
                                }
                            }
                            if (changed) break
                        }
                    }

                    val mergedBoxes = clusters.map { cluster ->
                        var left = Int.MAX_VALUE
                        var top = Int.MAX_VALUE
                        var right = Int.MIN_VALUE
                        var bottom = Int.MIN_VALUE
                        for (rect in cluster) {
                            left = minOf(left, rect.left)
                            top = minOf(top, rect.top)
                            right = maxOf(right, rect.right)
                            bottom = maxOf(bottom, rect.bottom)
                        }
                        
                        // Add some generous padding (e.g. 15% of width/height or fixed pixels)
                        val w = right - left
                        val h = bottom - top
                        val paddingX = (w * 0.15f).toInt().coerceAtLeast(20)
                        val paddingY = (h * 0.15f).toInt().coerceAtLeast(20)
                        
                        Rect(
                            maxOf(0, left - paddingX),
                            maxOf(0, top - paddingY),
                            minOf(bitmap.width, right + paddingX),
                            minOf(bitmap.height, bottom + paddingY)
                        )
                    }
                    detectedBoxes = mergedBoxes
                    if (mergedBoxes.isEmpty()) {
                        hideOcrBoxes = true
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    recognizer.close()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun confirmBitmapRegion(x: Int, y: Int, w: Int, h: Int) {
        if (w > 0 && h > 0) {
            try {
                onConfirm(Bitmap.createBitmap(bitmap, x, y, w, h))
            } catch (e: Throwable) {
                e.printStackTrace()
                onConfirm(bitmap)
            }
        } else {
            onConfirm(bitmap)
        }
    }

    fun confirmOcrBox(box: Rect) {
        val paddingX = (box.width() * 0.05f).toInt()
        val paddingY = (box.height() * 0.05f).toInt()
        val x = maxOf(0, box.left - paddingX)
        val y = maxOf(0, box.top - paddingY)
        val w = minOf(bitmap.width - x, box.width() + paddingX * 2)
        val h = minOf(bitmap.height - y, box.height() + paddingY * 2)
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
                                    cropState.activeHandle = DragHandle.NONE
                                    
                                    // If OCR boxes are visible, a long-press drag on a box corner edits that box.
                                    // Any other gesture submits the touched box, or the current crop if no box was touched.
                                    if (!hideOcrBoxes) {
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
                                    val canEdit = targetHandle != DragHandle.NONE

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val changes = event.changes

                                        if (changes.all { !it.pressed }) {
                                            if (isEditing) {
                                                cropState.stopDrag()
                                            } else {
                                                downEvent.consume()
                                                submitTarget()
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

                                            if (!isEditing && canEdit) {
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
                    if (hideOcrBoxes) {
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
                        // Draw OCR selection boxes with L-bracket lines at corners
                        detectedBoxes.forEach { box ->
                            val displayLeft = cropState.imgOffsetX + box.left * cropState.scaleFactor
                            val displayTop = cropState.imgOffsetY + box.top * cropState.scaleFactor
                            val displayRight = cropState.imgOffsetX + box.right * cropState.scaleFactor
                            val displayBottom = cropState.imgOffsetY + box.bottom * cropState.scaleFactor
                            
                            // 1. Semi-transparent background fill
                            drawRect(
                                color = KuriAmber.copy(alpha = 0.15f),
                                topLeft = Offset(displayLeft, displayTop),
                                size = Size(displayRight - displayLeft, displayBottom - displayTop)
                            )
                            
                            // 2. Thin golden border
                            drawRect(
                                color = KuriAmber.copy(alpha = 0.4f),
                                topLeft = Offset(displayLeft, displayTop),
                                size = Size(displayRight - displayLeft, displayBottom - displayTop),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                            
                            // 3. L-bracket corners
                            val lLength = 8.dp.toPx()
                            val lStroke = 2.dp.toPx()
                            
                            // TL
                            drawLine(KuriAmber, Offset(displayLeft, displayTop), Offset(displayLeft + lLength, displayTop), strokeWidth = lStroke)
                            drawLine(KuriAmber, Offset(displayLeft, displayTop), Offset(displayLeft, displayTop + lLength), strokeWidth = lStroke)
                            
                            // TR
                            drawLine(KuriAmber, Offset(displayRight, displayTop), Offset(displayRight - lLength, displayTop), strokeWidth = lStroke)
                            drawLine(KuriAmber, Offset(displayRight, displayTop), Offset(displayRight, displayTop + lLength), strokeWidth = lStroke)
                            
                            // BL
                            drawLine(KuriAmber, Offset(displayLeft, displayBottom), Offset(displayLeft + lLength, displayBottom), strokeWidth = lStroke)
                            drawLine(KuriAmber, Offset(displayLeft, displayBottom), Offset(displayLeft, displayBottom - lLength), strokeWidth = lStroke)
                            
                            // BR
                            drawLine(KuriAmber, Offset(displayRight, displayBottom), Offset(displayRight - lLength, displayBottom), strokeWidth = lStroke)
                            drawLine(KuriAmber, Offset(displayRight, displayBottom), Offset(displayRight, displayBottom - lLength), strokeWidth = lStroke)
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
                                        e.printStackTrace()
                                        onConfirm(bitmap)
                                    }
                                } else {
                                    onConfirm(bitmap)
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
                    fontSize = 16.sp
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
                                    e.printStackTrace()
                                    onConfirm(bitmap)
                                }
                            } else {
                                onConfirm(bitmap)
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

