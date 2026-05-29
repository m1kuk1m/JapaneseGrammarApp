package com.example.japanesegrammarapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.japanesegrammarapp.ui.AppNavigation
import com.example.japanesegrammarapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request the highest available refresh rate (120Hz / 90Hz) for smoother animations
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.let { win ->
                val maxMode = win.windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val attrs = win.attributes
                    attrs.preferredDisplayModeId = maxMode.modeId
                    win.attributes = attrs
                }
            }
        }

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
