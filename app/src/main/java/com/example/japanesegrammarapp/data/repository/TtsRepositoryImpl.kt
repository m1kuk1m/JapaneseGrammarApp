package com.example.japanesegrammarapp.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.japanesegrammarapp.domain.repository.TtsRepository
import com.example.japanesegrammarapp.domain.repository.SettingsRepository

@Singleton
class TtsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: SettingsRepository
) : TtsRepository {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    private var mediaPlayer: MediaPlayer? = null
    private var tempAudioFile: File? = null
    private var ttsJob: kotlinx.coroutines.Job? = null

    override fun playText(text: String) {
        ttsJob?.cancel() // Cancel the active network request/processing job
        stop() // Stop any ongoing playback
        _isPlaying.value = true

        val provider = settingsRepository.getTtsProvider()
        val url = settingsRepository.getTtsApiUrl(provider)
        val key = settingsRepository.getTtsApiKey(provider)
        val model = settingsRepository.getTtsModel(provider)
        val voice = settingsRepository.getTtsVoice(provider)
        val region = settingsRepository.getTtsRegion(provider)

        ttsJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                yield()
                tempAudioFile?.let { if (it.exists()) it.delete() }
                tempAudioFile = File(context.cacheDir, "tts_audio_cache.mp3")
                
                when (provider) {
                    "OpenAI" -> playOpenAi(text, url, key, model, voice)
                    "Google" -> playGoogle(text, url, key, voice)
                    "Microsoft" -> playMicrosoft(text, region, key, voice)
                    else -> throw Exception("Unknown TTS provider")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                com.example.japanesegrammarapp.utils.AppLogger.e("TTS", "TTS Failure: ${e.message}", e)
                _isPlaying.value = false
            }
        }
    }

    private suspend fun playOpenAi(text: String, url: String, key: String, model: String, voice: String) {
        val json = JSONObject().apply {
            put("model", model.ifBlank { "tts-1" })
            put("input", text)
            put("voice", voice.ifBlank { "alloy" })
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url.ifBlank { "https://api.openai.com/v1/audio/speech" })
            .addHeader("Authorization", "Bearer $key")
            .post(requestBody)
            .build()
            
        executeAudioRequest(request)
    }

    private suspend fun playGoogle(text: String, url: String, key: String, voice: String) {
        val json = JSONObject().apply {
            put("input", JSONObject().put("text", text))
            put("voice", JSONObject().put("languageCode", "ja-JP").put("name", voice.ifBlank { "ja-JP-Neural2-B" }))
            put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val baseUrl = url.ifBlank { "https://texttospeech.googleapis.com/v1/text:synthesize" }
        val requestUrl = if (baseUrl.contains("?")) "$baseUrl&key=$key" else "$baseUrl?key=$key"
        
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()
            
        yield()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.body?.string()}")
        }
        
        yield()
        val responseBodyString = response.body?.string() ?: throw Exception("Empty response body")
        val jsonResponse = JSONObject(responseBodyString)
        val audioContentBase64 = jsonResponse.optString("audioContent")
        if (audioContentBase64.isBlank()) {
            throw Exception("No audioContent in response")
        }
        
        val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
        yield()
        FileOutputStream(tempAudioFile).use { it.write(audioBytes) }
        yield()
        playAudioFile()
    }

    private suspend fun playMicrosoft(text: String, region: String, key: String, voice: String) {
        val ssml = "<speak version='1.0' xml:lang='ja-JP'><voice name='${voice.ifBlank { "ja-JP-NanamiNeural" }}'>${text.replace("<", "&lt;").replace(">", "&gt;")}</voice></speak>"
        val requestBody = ssml.toRequestBody("application/ssml+xml".toMediaType())
        val request = Request.Builder()
            .url("https://${region.ifBlank { "eastus" }}.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", key)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
            .post(requestBody)
            .build()
            
        executeAudioRequest(request)
    }

    private suspend fun executeAudioRequest(request: Request) {
        yield()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.body?.string()}")
        }
        
        yield()
        val inputStream = response.body?.byteStream() ?: throw Exception("Empty response body")
        FileOutputStream(tempAudioFile).use { output ->
            inputStream.copyTo(output)
        }
        yield()
        playAudioFile()
    }

    private fun playAudioFile() {
        if (tempAudioFile == null || !tempAudioFile!!.exists() || tempAudioFile!!.length() == 0L) {
            _isPlaying.value = false
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempAudioFile!!.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        it.release()
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlaying.value = false
                        true
                    }
                    start()
                }
            } catch (e: Exception) {
                _isPlaying.value = false
            }
        }
    }

    override fun stop() {
        ttsJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }
}
