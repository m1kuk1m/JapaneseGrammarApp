package com.example.japanesegrammarapp.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import kotlinx.coroutines.CancellationException
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
    ocrBoxDetectionSettings: OcrBoxDetectionSettings = OcrBoxDetectionSettings.DEFAULT,
    autoDeskewAfterCapture: Boolean = false,
    settingsLoaded: Boolean = true,
    uiPreferencesRepository: com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
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
    var captureDeviceOrientation by rememberSaveable { mutableStateOf(DeviceOrientation.PORTRAIT) }
    var screenMode by rememberSaveable { mutableStateOf(CameraScreenMode.CAPTURE) }
    var flashMode by rememberSaveable { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var tempFileUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var wasImageRotatedToPortrait by rememberSaveable { mutableStateOf(false) } // Keep for gallery fallback
    var captureStage by remember { mutableStateOf(CaptureStage.IDLE) }
    var isCameraReady by remember { mutableStateOf(false) }
    var deskewOutcome by remember { mutableStateOf<DeskewOutcome>(DeskewOutcome.Disabled) }
    val latestAutoDeskew by rememberUpdatedState(autoDeskewAfterCapture)
    val isBusy = captureStage != CaptureStage.IDLE

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
    LaunchedEffect(galleryImageUriString, settingsLoaded) {
        if (settingsLoaded && !galleryImageUriString.isNullOrBlank()) {
            captureStage = CaptureStage.PROCESSING
            val uri = Uri.parse(galleryImageUriString)
            try {
                val result = withContext(Dispatchers.IO) { loadCameraReviewBitmap(context, uri) }
                    ?: error("Unable to decode gallery image")
                val processed = withContext(Dispatchers.IO) {
                    applyAutoDeskewIfEnabled(result.bitmap, latestAutoDeskew)
                }
                replaceCapturedBitmap(processed.bitmap)
                deskewOutcome = processed.outcome
                wasImageRotatedToPortrait = result.wasRotatedToPortrait
                screenMode = CameraScreenMode.CROP_REVIEW
                showDeskewFailureIfNeeded(context, processed.outcome)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                AppLogger.e("CAMERA", "Failed to load gallery image", error)
                Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } finally {
                captureStage = CaptureStage.IDLE
            }
        }
    }

    // Restore captured image if saved state contains a path
    LaunchedEffect(tempFileUriString, settingsLoaded) {
        if (settingsLoaded && !tempFileUriString.isNullOrBlank() && capturedBitmap == null) {
            captureStage = CaptureStage.PROCESSING
            val uri = Uri.parse(tempFileUriString)
            try {
                val result = withContext(Dispatchers.IO) { loadCameraReviewBitmap(context, uri) }
                    ?: error("Unable to restore captured image")
                val processed = withContext(Dispatchers.IO) {
                    applyAutoDeskewIfEnabled(result.bitmap, latestAutoDeskew)
                }
                replaceCapturedBitmap(processed.bitmap)
                deskewOutcome = processed.outcome
                wasImageRotatedToPortrait = result.wasRotatedToPortrait
                screenMode = CameraScreenMode.CROP_REVIEW
                showDeskewFailureIfNeeded(context, processed.outcome)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                AppLogger.e("CAMERA", "Failed to restore captured image", error)
                Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
            } finally {
                captureStage = CaptureStage.IDLE
            }
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

    val canCapture = settingsLoaded && isCameraReady && captureStage == CaptureStage.IDLE
    val performCapture: () -> Boolean = remember(imageCapture, deviceOrientation, canCapture, latestAutoDeskew) {
        {
            if (canCapture) {
                captureDeviceOrientation = deviceOrientation
                captureStage = CaptureStage.CAPTURING
                val autoDeskewForCapture = latestAutoDeskew
                try {
                    val file = createCameraCaptureFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                captureStage = CaptureStage.PROCESSING
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val result = processCapturedImageFile(context, file)
                                            ?: error("Unable to decode captured image")
                                        val processed = applyAutoDeskewIfEnabled(result.bitmap, autoDeskewForCapture)

                                        withContext(Dispatchers.Main) {
                                            replaceCapturedBitmap(processed.bitmap)
                                            deskewOutcome = processed.outcome
                                            wasImageRotatedToPortrait = result.wasRotatedToPortrait
                                            tempFileUriString = result.savedUri?.toString()
                                            screenMode = CameraScreenMode.CROP_REVIEW
                                            showDeskewFailureIfNeeded(context, processed.outcome)
                                        }
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Throwable) {
                                        AppLogger.e("CAMERA", "Capture processing failed", error)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            captureStage = CaptureStage.IDLE
                                        }
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                AppLogger.e("CAMERA", "Capture failed: ${exception.message}", exception)
                                captureStage = CaptureStage.IDLE
                                Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    true
                } catch (error: Throwable) {
                    AppLogger.e("CAMERA", "Unable to start capture", error)
                    captureStage = CaptureStage.IDLE
                    Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                    false
                }
            } else {
                false
            }
        }
    }

    DisposableEffect(screenMode, hasCameraPermission, performCapture) {
        val mainActivity = context as? com.example.japanesegrammarapp.MainActivity
        if (mainActivity != null && screenMode == CameraScreenMode.CAPTURE && hasCameraPermission) {
            mainActivity.onVolumeKeyDownListener = {
                performCapture()
                true
            }
        }
        onDispose {
            (context as? com.example.japanesegrammarapp.MainActivity)?.let {
                it.onVolumeKeyDownListener = null
            }
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
                            captureEnabled = canCapture,
                            onCameraReadyChanged = { isCameraReady = it },
                            onCapture = { performCapture() },
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
                            originalBitmap = bitmap,
                            captureDeviceOrientation = captureDeviceOrientation,
                            ocrBoxDetectionSettings = ocrBoxDetectionSettings,
                            autoDeskewAfterCapture = autoDeskewAfterCapture,
                            initialDeskewOutcome = deskewOutcome,
                            uiPreferencesRepository = uiPreferencesRepository,
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
                                captureStage = CaptureStage.PROCESSING
                                val sourceBitmap = capturedBitmap
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val outUri = saveConfirmedCrop(context, croppedBitmap, false)
                                            ?: error("Unable to save cropped image")
                                        withContext(Dispatchers.Main) {
                                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                                "captured_image_uri",
                                                outUri.toString()
                                            )
                                            replaceCapturedBitmap(null)
                                            tempFileUriString = null
                                            navController.popBackStack()
                                        }
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Throwable) {
                                        AppLogger.e("CAMERA", "Failed to save confirmed crop", error)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        if (croppedBitmap !== sourceBitmap && !croppedBitmap.isRecycled) {
                                            croppedBitmap.recycle()
                                        }
                                        withContext(Dispatchers.Main) {
                                            captureStage = CaptureStage.IDLE
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Loading Overlay
            if (isBusy) {
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

private suspend fun applyAutoDeskewIfEnabled(bitmap: Bitmap, enabled: Boolean): DeskewProcessingResult {
    if (!enabled) return DeskewProcessingResult(bitmap, DeskewOutcome.Disabled)
    return when (val detection = detectImageSkew(bitmap)) {
        is SkewDetectionResult.Detected -> {
            if (kotlin.math.abs(detection.angle) <= 0.5f) {
                DeskewProcessingResult(bitmap, DeskewOutcome.NotNeeded(detection.angle))
            } else {
                try {
                    val rotated = rotateBitmapPreservingContent(bitmap, -detection.angle)
                    if (rotated !== bitmap && !bitmap.isRecycled) bitmap.recycle()
                    DeskewProcessingResult(rotated, DeskewOutcome.Corrected(detection.angle))
                } catch (error: Throwable) {
                    AppLogger.e("CAMERA", "Auto deskew rotation failed", error)
                    DeskewProcessingResult(bitmap, DeskewOutcome.Failed(error))
                }
            }
        }
        SkewDetectionResult.NoText -> DeskewProcessingResult(bitmap, DeskewOutcome.NoText)
        is SkewDetectionResult.Failed -> DeskewProcessingResult(bitmap, DeskewOutcome.Failed(detection.cause))
        SkewDetectionResult.TimedOut -> DeskewProcessingResult(bitmap, DeskewOutcome.TimedOut)
    }
}

private fun showDeskewFailureIfNeeded(context: android.content.Context, outcome: DeskewOutcome) {
    if (outcome is DeskewOutcome.Failed || outcome == DeskewOutcome.TimedOut) {
        Toast.makeText(context, R.string.camera_auto_deskew_failed, Toast.LENGTH_SHORT).show()
    }
}
