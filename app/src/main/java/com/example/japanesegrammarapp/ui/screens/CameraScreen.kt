package com.example.japanesegrammarapp.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import com.example.japanesegrammarapp.utils.AppLogger
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.japanesegrammarapp.domain.model.OcrBoxDetectionSettings

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CameraScreen(
    navController: NavController,
    galleryImageUriString: String? = null,
    ocrBoxDetectionSettings: OcrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT
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
            .setResolutionSelector(cameraResolutionSelector())
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
            val bitmap = loadCameraReviewBitmap(context, uri)
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
            val bitmap = loadCameraReviewBitmap(context, uri)
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
                                val file = createCameraCaptureFile(context)
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
                                                val result = processCapturedImageFile(context, file)
                                                if (result != null) {
                                                    withContext(Dispatchers.Main) {
                                                        replaceCapturedBitmap(result.bitmap)
                                                        tempFileUriString = result.savedUri?.toString()
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
                                            AppLogger.e("CAMERA", "Capture failed: ${exception.message}", exception)
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
                        CameraPermissionState(
                            showGoToSettings = showGoToSettings,
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onOpenSettings = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                } else {
                    // Interactive review and crop state
                    capturedBitmap?.let { bitmap ->
                        ImageCropReviewLayout(
                            bitmap = bitmap,
                            ocrBoxDetectionSettings = ocrBoxDetectionSettings,
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
                                    val outUri = saveConfirmedCrop(context, croppedBitmap)
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
