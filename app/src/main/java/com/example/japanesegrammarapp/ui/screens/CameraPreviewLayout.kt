package com.example.japanesegrammarapp.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.utils.AppLogger

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
            .setResolutionSelector(cameraResolutionSelector())
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
                AppLogger.e("CAMERA", "Failed to unbind camera on dispose", e)
            }
        }
    }

    LaunchedEffect(deviceOrientation) {
        try {
            imageCapture.targetRotation = deviceOrientation.targetRotation
            preview.targetRotation = deviceOrientation.targetRotation
        } catch (e: Exception) {
            AppLogger.e("CAMERA", "Failed to update target rotation dynamically", e)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

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
                        AppLogger.e("CAMERA", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        ZenScanningOverlay()

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

        val (flashVectorIcon, flashTextRes, flashDescRes) = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> Triple(Icons.Rounded.FlashOn, R.string.camera_flash_on_label, R.string.camera_flash_on_desc)
            ImageCapture.FLASH_MODE_AUTO -> Triple(Icons.Rounded.FlashAuto, R.string.camera_flash_auto_label, R.string.camera_flash_auto_desc)
            else -> Triple(Icons.Rounded.FlashOff, R.string.camera_flash_off_label, R.string.camera_flash_off_desc)
        }
        val labelText = stringResource(flashTextRes).trim()

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

        val laserY = top + ((bottom - top) * laserYOffsetProgress)
        drawLine(
            color = KuriAmber.copy(alpha = 0.7f),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
