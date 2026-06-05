package com.example.japanesegrammarapp

import android.os.Bundle
import android.content.Intent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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
import androidx.compose.ui.res.stringResource
import com.example.japanesegrammarapp.ui.AppNavigation
import com.example.japanesegrammarapp.ui.SettingsViewModel
import com.example.japanesegrammarapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val externalTextChannel = Channel<String>(Channel.BUFFERED)
    val externalTextFlow = externalTextChannel.receiveAsFlow()

    private val intentChannel = Channel<Intent>(Channel.BUFFERED)
    val intentFlow = intentChannel.receiveAsFlow()

    @androidx.compose.foundation.ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        // Request the highest available refresh rate (120Hz / 90Hz) on the next frame loop to avoid blocking onCreate startup
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.post {
                try {
                    window.let { win ->
                        val maxMode = win.windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }
                        if (maxMode != null) {
                            val attrs = win.attributes
                            attrs.preferredDisplayModeId = maxMode.modeId
                            win.attributes = attrs
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                darkTheme = isDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // If wallpaper is set, we use transparent background for Surface so the image shows through
                    color = if (uiState.wallpaperUri.isNotBlank()) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    if (uiState.wallpaperUri.isNotBlank()) {
                        AsyncImage(
                            model = uiState.wallpaperUri,
                            contentDescription = stringResource(R.string.cd_background_wallpaper),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    AppNavigation(externalTextFlow = externalTextFlow, intentFlow = intentFlow)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intentChannel.trySend(intent)
        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        text?.takeIf { it.isNotBlank() }?.let {
            externalTextChannel.trySend(it)
        }
    }
}
