package com.example.japanesegrammarapp.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.AppDatabase
import com.example.japanesegrammarapp.network.*
import com.example.japanesegrammarapp.vision.OcrHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    object NavigateToResult : UiEvent()
}

class AppViewModel(private val context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val analysisDao = database.analysisDao()
    private val ocrHelper = OcrHelper()
    private val llmService = ApiClient.llmService

    private val defaultUrls = mapOf(
        "Gemini" to "https://generativelanguage.googleapis.com/v1beta",
        "Vertex AI" to "https://aiplatform.googleapis.com/v1/publishers/google",
        "DeepSeek" to "https://api.deepseek.com",
        "Qwen" to "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "OpenAI Compatible" to "https://api.openai.com/v1"
    )

    val history = analysisDao.getAllRecords()

    private val _selectedRecord = MutableStateFlow<AnalysisRecord?>(null)
    val selectedRecord: StateFlow<AnalysisRecord?> = _selectedRecord.asStateFlow()

    init {
        viewModelScope.launch {
            history.collect { recordList ->
                val currentSelected = _selectedRecord.value
                if (currentSelected != null) {
                    val updated = recordList.find { it.id == currentSelected.id }
                    if (updated != null && (updated.status != currentSelected.status || updated.analysisResult != currentSelected.analysisResult)) {
                        _selectedRecord.value = updated
                        _analysisResult.value = updated.analysisResult
                        if (updated.analysisResult != null) {
                            try {
                                val cleanJson = updated.analysisResult.trim()
                                    .removePrefix("```json")
                                    .removePrefix("```text")
                                    .removePrefix("```")
                                    .removeSuffix("```")
                                    .trim()
                                _detailedResult.value = gson.fromJson(cleanJson, DetailedAnalysisResult::class.java)
                            } catch (e: Exception) {
                                _detailedResult.value = null
                            }
                        } else {
                            _detailedResult.value = null
                        }
                    }
                }
            }
        }
    }

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _currentOriginalText = MutableStateFlow("")
    val currentOriginalText: StateFlow<String> = _currentOriginalText.asStateFlow()

    fun setCurrentOriginalText(text: String) {
        _currentOriginalText.value = text
    }

    private val settingPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _useOcr = MutableStateFlow(settingPrefs.getBoolean("use_ocr", true))
    val useOcr: StateFlow<Boolean> = _useOcr.asStateFlow()

    fun setUseOcr(value: Boolean) {
        _useOcr.value = value
        settingPrefs.edit().putBoolean("use_ocr", value).apply()
    }

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private val gson = com.google.gson.Gson()

    private val _detailedResult = MutableStateFlow<DetailedAnalysisResult?>(null)
    val detailedResult: StateFlow<DetailedAnalysisResult?> = _detailedResult.asStateFlow()

    fun selectRecord(record: AnalysisRecord) {
        _selectedRecord.value = record
        _currentOriginalText.value = record.originalText
        _analysisResult.value = record.analysisResult
        try {
            val resultText = record.analysisResult
            if (resultText != null) {
                val cleanJson = resultText.trim()
                    .removePrefix("```json")
                    .removePrefix("```text")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                _detailedResult.value = gson.fromJson(cleanJson, DetailedAnalysisResult::class.java)
            } else {
                _detailedResult.value = null
            }
        } catch (e: Exception) {
            _detailedResult.value = null
        }
    }

    fun clearSelectedRecord() {
        _selectedRecord.value = null
        _currentOriginalText.value = ""
        _analysisResult.value = null
        _detailedResult.value = null
    }

    // ============================================================
    // Known model lists for providers that don't support GET /models
    // ============================================================

    private val qwenKnownModels = listOf(
        "qwen-max", "qwen-max-latest",
        "qwen-plus", "qwen-plus-latest",
        "qwen-turbo", "qwen-turbo-latest",
        "qwen-long",
        "qwen-vl-max", "qwen-vl-plus",
        "qwen2.5-72b-instruct", "qwen2.5-32b-instruct", "qwen2.5-14b-instruct", "qwen2.5-7b-instruct"
    )

    private val geminiKnownModels = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.5-flash-lite",
        "gemini-1.5-flash",
        "gemini-1.5-pro"
    )

    // ============================================================
    // Fetch models
    // ============================================================

    fun fetchModels(provider: String, baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) {
                _uiEvent.send(UiEvent.ShowError("Please configure API Key in Settings first."))
                return@launch
            }
            val effectiveUrl = if (baseUrl.isBlank()) defaultUrls[provider] ?: "" else baseUrl
            _isFetchingModels.value = true
            try {
                when (provider) {
                    "DeepSeek", "OpenAI Compatible" -> {
                        val cleanBase = effectiveUrl.trimEnd('/')
                        val effectiveBase = if (cleanBase.endsWith("/v1")) cleanBase.dropLast(3) else cleanBase
                        val url = "$effectiveBase/models"
                        val response = llmService.getOpenAiModels(url, "Bearer $apiKey")
                        _availableModels.value = response.data.map { it.id }
                    }
                    "Qwen" -> {
                        _availableModels.value = qwenKnownModels
                    }
                    "Gemini", "Vertex AI" -> {
                        try {
                            val cleanBase = effectiveUrl.trimEnd('/')
                            val url = "$cleanBase/models?key=$apiKey"
                            val response = llmService.getGeminiModels(url)
                            
                            _availableModels.value = response.models
                                .filter { model ->
                                    model.supportedGenerationMethods?.contains("generateContent") ?: true
                                }
                                .map { it.name.removePrefix("models/") }
                        } catch (e: Exception) {
                            _availableModels.value = geminiKnownModels
                        }
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                _uiEvent.send(UiEvent.ShowError("HTTP ${e.code()}: ${e.message()}\n$errorBody"))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowError("Network Error: ${e.localizedMessage}"))
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    // ============================================================
    // Analyze text (Concurrent Background Queue)
    // ============================================================

    fun analyzeText(
        text: String,
        imageUri: Uri?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ) {
        viewModelScope.launch {
            if (text.isBlank() && imageUri == null) {
                _uiEvent.send(UiEvent.ShowError("Please enter text or capture an image."))
                return@launch
            }
            if (apiKey.isBlank()) {
                _uiEvent.send(UiEvent.ShowError("Missing API Key."))
                return@launch
            }

            // 1. Insert PENDING record
            val record = AnalysisRecord(
                originalText = text.ifBlank { "画像文法分析" },
                imageUri = imageUri?.toString(),
                analysisResult = null,
                modelUsed = "$provider: $modelName",
                status = "PENDING"
            )
            val recordId = analysisDao.insert(record).toInt()
            val insertedRecord = record.copy(id = recordId)

            // 2. Set selected record to show pending loader state and do not clear input fields immediately
            _selectedRecord.value = insertedRecord
            _uiEvent.send(UiEvent.ShowError("分析タスクを開始しました。"))

            // 3. Launch background worker
            launchBackgroundAnalysis(recordId, text, imageUri, provider, modelName, baseUrl, apiKey)
        }
    }

    fun retryAnalysis(recordId: Int) {
        viewModelScope.launch {
            val record = analysisDao.getRecordById(recordId) ?: return@launch
            analysisDao.update(record.copy(status = "PENDING", errorMessage = null))

            val providerAndModel = record.modelUsed.split(": ")
            val provider = providerAndModel.getOrNull(0) ?: "Gemini"
            val modelName = providerAndModel.getOrNull(1) ?: "default"

            val prefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
            val key = prefs.getString("${provider}_key", "") ?: ""
            val url = prefs.getString("${provider}_url", "") ?: ""
            val imageUri = record.imageUri?.let { Uri.parse(it) }

            launchBackgroundAnalysis(recordId, record.originalText, imageUri, provider, modelName, url, key)
        }
    }

    fun deleteRecord(record: AnalysisRecord) {
        viewModelScope.launch {
            analysisDao.delete(record)
        }
    }

    private fun launchBackgroundAnalysis(
        recordId: Int,
        text: String,
        imageUri: Uri?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveUrl = if (baseUrl.isBlank()) defaultUrls[provider] ?: "" else baseUrl
                val isOcrEnabled = _useOcr.value
                var imageBase64: String? = null
                var mimeType: String? = "image/jpeg"
                if (!isOcrEnabled && imageUri != null) {
                    val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                    if (bytes != null) {
                        imageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                    }
                }

                val finalPrompt = text.ifBlank { "画像内のすべての日本語の文法構造と語彙を詳細に分析してください。" }

                val result = when (provider) {
                    "DeepSeek", "OpenAI Compatible", "Qwen" -> {
                        val cleanBase = effectiveUrl.trimEnd('/')
                        val url = "$cleanBase/chat/completions"

                        val contentPayload: Any = if (imageBase64 != null) {
                            listOf(
                                OpenAiContentPart(type = "text", text = PromptManager.SYSTEM_PROMPT + "\n\n分析対象:\n" + finalPrompt),
                                OpenAiContentPart(
                                    type = "image_url",
                                    image_url = OpenAiImageUrl(url = "data:$mimeType;base64,$imageBase64")
                                )
                            )
                        } else {
                            PromptManager.SYSTEM_PROMPT + "\n\n分析対象:\n" + finalPrompt
                        }

                        val request = OpenAiRequest(
                            model = modelName,
                            messages = listOf(
                                OpenAiMessage(role = "user", content = contentPayload)
                            )
                        )
                        val response = llmService.generateOpenAiCompatible(url, "Bearer $apiKey", request)
                        response.choices.firstOrNull()?.message?.content ?: "No response from model"
                    }
                    "Gemini", "Vertex AI" -> {
                        val cleanBase = effectiveUrl.trimEnd('/')
                        val url = "$cleanBase/models/$modelName:generateContent?key=$apiKey"

                        val parts = mutableListOf<GeminiPart>()
                        parts.add(GeminiPart(text = finalPrompt))
                        if (imageBase64 != null && mimeType != null) {
                            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = imageBase64)))
                        }

                        val request = GeminiRequest(
                            contents = listOf(GeminiContent(role = "user", parts = parts)),
                            systemInstruction = GeminiContent(role = "user", parts = listOf(GeminiPart(text = PromptManager.SYSTEM_PROMPT)))
                        )
                        val response = llmService.generateGemini(url, request)
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from model"
                    }
                    else -> "Unsupported provider"
                }

                val currentRecord = analysisDao.getRecordById(recordId)
                if (currentRecord != null) {
                    analysisDao.update(
                        currentRecord.copy(
                            analysisResult = result,
                            status = "COMPLETED"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = when (e) {
                    is retrofit2.HttpException -> {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        "HTTP ${e.code()}: ${e.message()}\n$body"
                    }
                    else -> e.localizedMessage ?: "Unknown network error"
                }
                val currentRecord = analysisDao.getRecordById(recordId)
                if (currentRecord != null) {
                    analysisDao.update(
                        currentRecord.copy(
                            status = "FAILED",
                            errorMessage = errorMsg
                        )
                    )
                }
            }
        }
    }

    suspend fun extractTextFromImage(uri: Uri): String {
        return ocrHelper.extractTextFromUri(context, uri)
    }
}
