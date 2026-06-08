package com.example.japanesegrammarapp.ui.screens

import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg

enum class DeviceOrientation(val rotationDegrees: Float, val targetRotation: Int) {
    PORTRAIT(0f, Surface.ROTATION_0),
    LANDSCAPE_LEFT(270f, Surface.ROTATION_90),
    LANDSCAPE_RIGHT(90f, Surface.ROTATION_270),
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

fun cameraResolutionSelector(): ResolutionSelector {
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        .build()
}

@Composable
fun CameraPermissionState(
    showGoToSettings: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("camera-permission-state")
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
                    onOpenSettings()
                } else {
                    onRequestPermission()
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
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
        ) {
            Text(stringResource(R.string.back), fontWeight = FontWeight.SemiBold)
        }
    }
}
