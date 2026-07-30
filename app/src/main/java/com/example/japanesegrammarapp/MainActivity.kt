package com.example.japanesegrammarapp

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.domain.usecase.AnalyzeTextUseCase
import com.example.japanesegrammarapp.domain.usecase.SaveAnalysisRecordUseCase
import com.example.japanesegrammarapp.ui.AppNavigation
import com.example.japanesegrammarapp.ui.SettingsViewModel
import com.example.japanesegrammarapp.ui.theme.AppTheme
import com.example.japanesegrammarapp.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var analyzeTextUseCase: AnalyzeTextUseCase

    @Inject
    lateinit var saveAnalysisRecordUseCase: SaveAnalysisRecordUseCase

    private val externalTextChannel = Channel<String>(Channel.BUFFERED)
    val externalTextFlow = externalTextChannel.receiveAsFlow()

    private val intentChannel = Channel<Intent>(Channel.BUFFERED)
    val intentFlow = intentChannel.receiveAsFlow()

    var onVolumeKeyDownListener: (() -> Boolean)? = null

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            val listener = onVolumeKeyDownListener
            if (listener != null) {
                if ((event?.repeatCount ?: 0) == 0) {
                    listener.invoke()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @androidx.compose.foundation.ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (handleIntent(intent)) {
            finish()
            overridePendingTransition(0, 0)
            return
        }

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
                    AppLogger.e("MAIN", "Failed to apply preferred display refresh rate", e)
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
        if (handleIntent(intent)) {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private fun handleIntent(intent: Intent): Boolean {
        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        if (!text.isNullOrBlank()) {
            val trimmedText = text.trim()
            if (::settingsRepository.isInitialized && ::saveAnalysisRecordUseCase.isInitialized) {
                val existingRecord = runBlocking(Dispatchers.IO) {
                    saveAnalysisRecordUseCase.getByOriginalText(trimmedText)
                }
                val isAlreadyAnalyzed = existingRecord != null && existingRecord.status == AnalysisStatus.COMPLETED

                if (!isAlreadyAnalyzed && settingsRepository.getSilentBackgroundMode()) {
                    intent.action = null
                    intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
                    intent.removeExtra(Intent.EXTRA_TEXT)

                    val provider = settingsRepository.getActiveProvider()
                    val savedModel = settingsRepository.getActiveModel(provider)
                    val model = if (savedModel.isNotBlank()) savedModel else {
                        val models = settingsRepository.getModelsForProvider(provider)
                        models.firstOrNull() ?: "default"
                    }
                    val key = settingsRepository.getApiKey(provider)
                    val url = settingsRepository.getApiUrl(provider)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            analyzeTextUseCase.execute(trimmedText, null, provider, model, url, key)
                        } catch (e: Exception) {
                            AppLogger.e("MAIN", "Silent background analysis failed to start", e)
                        }
                    }

                    val displayText = if (trimmedText.length > 15) trimmedText.take(15) + "..." else trimmedText
                    val toastMsg = getString(R.string.silent_analysis_queued_toast, displayText)
                    Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
                    return true
                }
            }

            externalTextChannel.trySend(trimmedText)
            intent.action = null
            intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
            intent.removeExtra(Intent.EXTRA_TEXT)
        }

        intentChannel.trySend(intent)
        return false
    }
}
