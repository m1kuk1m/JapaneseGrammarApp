package com.example.japanesegrammarapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.japanesegrammarapp.ui.AppNavigation
import com.example.japanesegrammarapp.ui.SettingsViewModel
import com.example.japanesegrammarapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            val isDarkTheme = when (uiState.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            AppTheme(
                darkTheme = isDarkTheme,
                primaryColorHex = uiState.primaryColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // If wallpaper is set, we use transparent background for Surface so the image shows through
                    color = if (uiState.wallpaperUri.isNotBlank()) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    if (uiState.wallpaperUri.isNotBlank()) {
                        AsyncImage(
                            model = uiState.wallpaperUri,
                            contentDescription = "Background Wallpaper",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    AppNavigation()
                }
            }
        }
    }

    override fun recreate() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
