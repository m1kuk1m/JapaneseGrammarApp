package com.example.japanesegrammarapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.domain.usecase.AnalyzeTextUseCase
import com.example.japanesegrammarapp.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExternalTextReceiverActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var analyzeTextUseCase: AnalyzeTextUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        if (!text.isNullOrBlank()) {
            val trimmedText = text.trim()
            val silentMode = settingsRepository.getSilentBackgroundMode()

            if (silentMode) {
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
                        AppLogger.e("EXTERNAL_RECEIVER", "Silent background analysis failed to start", e)
                    }
                }

                val displayText = if (trimmedText.length > 15) trimmedText.take(15) + "..." else trimmedText
                val toastMsg = getString(R.string.silent_analysis_queued_toast, displayText)
                Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_SHORT).show()

                finish()
                overridePendingTransition(0, 0)
                return
            } else {
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(mainIntent)
                finish()
                overridePendingTransition(0, 0)
                return
            }
        }

        finish()
        overridePendingTransition(0, 0)
    }
}
