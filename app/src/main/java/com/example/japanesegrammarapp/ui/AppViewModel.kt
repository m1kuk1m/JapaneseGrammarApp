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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

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

    private val masterKey = androidx.security.crypto.MasterKey.Builder(context)
        .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
        context,
        "api_keys_secure",
        masterKey,
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val defaultModels = mapOf(
        "Gemini" to listOf("gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro"),
        "Vertex AI" to listOf("gemini-1.5-flash", "gemini-1.5-pro"),
        "DeepSeek" to listOf("deepseek-chat", "deepseek-coder"),
        "Qwen" to listOf("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-32b-instruct", "qwen2.5-14b-instruct", "qwen2.5-7b-instruct"),
        "OpenAI Compatible" to listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")
    )

    private val _activeProvider = MutableStateFlow(securePrefs.getString("active_provider", "Gemini") ?: "Gemini")
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    private val _activeModel = MutableStateFlow("")
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _providerModels = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val providerModels: StateFlow<Map<String, List<String>>> = _providerModels.asStateFlow()

    val history = analysisDao.getAllRecords()

    private val _selectedRecord = MutableStateFlow<AnalysisRecord?>(null)
    val selectedRecord: StateFlow<AnalysisRecord?> = _selectedRecord.asStateFlow()

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

    private val _fetchingProvider = MutableStateFlow<String?>(null)
    val fetchingProvider: StateFlow<String?> = _fetchingProvider.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val gson = com.google.gson.Gson()

    private val _detailedResult = MutableStateFlow<DetailedAnalysisResult?>(null)
    val detailedResult: StateFlow<DetailedAnalysisResult?> = _detailedResult.asStateFlow()

    fun getModelsForProvider(provider: String): List<String> {
        val json = securePrefs.getString("${provider}_models_list_json", null)
        return if (json != null) {
            try {
                val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson(json, listType) ?: (defaultModels[provider] ?: emptyList())
            } catch (e: Exception) {
                defaultModels[provider] ?: emptyList()
            }
        } else {
            defaultModels[provider] ?: emptyList()
        }
    }

    fun saveModelsForProvider(provider: String, models: List<String>) {
        securePrefs.edit().putString("${provider}_models_list_json", gson.toJson(models)).apply()
        _providerModels.value = _providerModels.value.toMutableMap().apply {
            put(provider, models)
        }
        if (_activeProvider.value == provider) {
            _availableModels.value = models
            val currentModel = _activeModel.value
            if (!models.contains(currentModel)) {
                val fallback = models.firstOrNull() ?: ""
                setActiveModel(fallback)
            }
        }
    }

    fun setActiveProvider(provider: String) {
        _activeProvider.value = provider
        securePrefs.edit().putString("active_provider", provider).apply()
        
        val models = getModelsForProvider(provider)
        _availableModels.value = models
        
        val selectedModel = securePrefs.getString("${provider}_selected_model", "") ?: ""
        if (selectedModel.isNotBlank() && models.contains(selectedModel)) {
            _activeModel.value = selectedModel
        } else {
            val fallback = models.firstOrNull() ?: ""
            _activeModel.value = fallback
            securePrefs.edit().putString("${provider}_selected_model", fallback).apply()
        }
    }

    fun setActiveModel(model: String) {
        _activeModel.value = model
        securePrefs.edit().putString("${_activeProvider.value}_selected_model", model).apply()
    }

    init {
        // Initialize provider models mapping
        val initialModels = defaultUrls.keys.associateWith { getModelsForProvider(it) }
        _providerModels.value = initialModels

        // Initialize active provider & model
        val provider = _activeProvider.value
        val models = initialModels[provider] ?: emptyList()
        _availableModels.value = models
        
        val selectedModel = securePrefs.getString("${provider}_selected_model", "") ?: ""
        if (selectedModel.isNotBlank() && models.contains(selectedModel)) {
            _activeModel.value = selectedModel
        } else {
            _activeModel.value = models.firstOrNull() ?: ""
        }

        viewModelScope.launch {
            history.collect { recordList ->
                val currentSelected = _selectedRecord.value
                if (currentSelected != null) {
                    val updated = recordList.find { it.id == currentSelected.id }
                    if (updated != null && (updated.status != currentSelected.status || updated.analysisResult != currentSelected.analysisResult || updated.originalText != currentSelected.originalText)) {
                        _selectedRecord.value = updated
                        _currentOriginalText.value = updated.originalText
                        _analysisResult.value = updated.analysisResult
                        viewModelScope.launch(Dispatchers.IO) {
                            val detail = parseDetailedResult(updated.originalText, updated.analysisResult)
                            _detailedResult.value = detail
                        }
                    }
                }
            }
        }

        // DEBUG: print raw Kuromoji tokens + bunsetsu result to Logcat on startup
        // Filter by tag "JapaneseSegmenter" in Android Studio Logcat to see output.
        // Remove this block once segmentation is verified.
        viewModelScope.launch(Dispatchers.IO) {
            JapaneseSegmenter.debugTokens("ここは事件性がないからと安易に切り捨てた俺の反省点だろうか。")
            JapaneseSegmenter.debugTokens("図書館で本を読んでいる。")
        }
    }

    fun selectRecord(record: AnalysisRecord) {
        _selectedRecord.value = record
        _currentOriginalText.value = record.originalText
        _analysisResult.value = record.analysisResult
        viewModelScope.launch(Dispatchers.IO) {
            val detail = parseDetailedResult(record.originalText, record.analysisResult)
            _detailedResult.value = detail
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
                _uiEvent.emit(UiEvent.ShowError("Please configure API Key in Settings first."))
                return@launch
            }
            val effectiveUrl = if (baseUrl.isBlank()) defaultUrls[provider] ?: "" else baseUrl
            _isFetchingModels.value = true
            _fetchingProvider.value = provider
            try {
                when (provider) {
                    "DeepSeek", "OpenAI Compatible" -> {
                        val cleanBase = effectiveUrl.trimEnd('/')
                        val url = "$cleanBase/models"
                        val response = llmService.getOpenAiModels(url, "Bearer $apiKey")
                        val fetched = response.data.map { it.id }
                        saveModelsForProvider(provider, fetched)
                    }
                    "Qwen" -> {
                        saveModelsForProvider(provider, qwenKnownModels)
                    }
                    "Gemini", "Vertex AI" -> {
                        try {
                            val cleanBase = effectiveUrl.trimEnd('/')
                            val url = "$cleanBase/models?key=$apiKey"
                            val response = llmService.getGeminiModels(url)
                            
                            val fetched = response.models
                                .filter { model ->
                                    model.supportedGenerationMethods?.contains("generateContent") ?: true
                                }
                                .map { it.name.removePrefix("models/") }
                            saveModelsForProvider(provider, fetched)
                        } catch (e: Exception) {
                            saveModelsForProvider(provider, geminiKnownModels)
                        }
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                _uiEvent.emit(UiEvent.ShowError("HTTP ${e.code()}: ${e.message()}\n$errorBody"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Network Error: ${e.localizedMessage}"))
            } finally {
                _isFetchingModels.value = false
                _fetchingProvider.value = null
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
                _uiEvent.emit(UiEvent.ShowError("Please enter text or capture an image."))
                return@launch
            }
            if (apiKey.isBlank()) {
                _uiEvent.emit(UiEvent.ShowError("Missing API Key."))
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
            _uiEvent.emit(UiEvent.ShowError("分析タスクを開始しました。"))

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

            val key = securePrefs.getString("${provider}_key", "") ?: ""
            val url = securePrefs.getString("${provider}_url", "") ?: ""
            val imageUri = record.imageUri?.let { Uri.parse(it) }

            launchBackgroundAnalysis(recordId, record.originalText, imageUri, provider, modelName, url, key)
        }
    }

    fun deleteRecord(record: AnalysisRecord) {
        viewModelScope.launch {
            analysisDao.delete(record)
        }
    }

    private fun parseDetailedResult(originalText: String, jsonString: String?): DetailedAnalysisResult? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            var cleanJson = jsonString.trim()
            if (cleanJson.startsWith("```")) {
                val firstNewLine = cleanJson.indexOf('\n')
                cleanJson = if (firstNewLine != -1) {
                    cleanJson.substring(firstNewLine).trim()
                } else {
                    cleanJson.removePrefix("```").trim()
                }
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.removeSuffix("```").trim()
            }
            val initialResult = gson.fromJson(cleanJson, DetailedAnalysisResult::class.java) ?: return null

            // 智能对齐兜底逻辑 (Smart Alignment Fallback)
            if (originalText.isNotBlank() && originalText != "画像文法分析") {
                val expectedTokens = JapaneseSegmenter.segmentAndCombine(originalText)
                if (expectedTokens.isNotEmpty()) {
                    val apiSegments = initialResult.segments.orEmpty()

                    if (apiSegments.size == expectedTokens.size) {
                        // 长度完美一致，但可能存在微小字符差异，我们以本地分词为准进行对齐和纠正
                        val alignedSegments = apiSegments.mapIndexed { idx, segment ->
                            if (segment.text != expectedTokens[idx]) {
                                segment.copy(text = expectedTokens[idx])
                            } else {
                                segment
                            }
                        }
                        initialResult.copy(segments = alignedSegments)
                    } else {
                        // Bug #8 修复：长度不一致时强行按索引对齐会导致数据错乱，因此直接返回初始结果，不作对齐
                        initialResult
                    }
                } else {
                    initialResult
                }
            } else {
                initialResult
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
                    val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        imageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                    }
                }

                val preSegments = if (text.isNotBlank() && text != "画像文法分析") {
                    JapaneseSegmenter.segmentAndCombine(text)
                } else {
                    emptyList()
                }

                val promptTrans = if (text.isNotBlank() && text != "画像文法分析") {
                    "分析対象の文: \"$text\"\nこの文の自然な中国語訳を出力してください。"
                } else {
                    "画像内のすべての日本語の文法構造と語彙を詳細に分析してください。"
                }

                val promptSeg = if (preSegments.isNotEmpty()) {
                    val segmentsJson = gson.toJson(preSegments)
                    """
                        分析対象の文: "$text"

                        【極めて重要な制約条件】
                        あなたは日本語テキストの「分かち書き（セグメンテーション）」を行う必要はありません。私がすでに以下の通りに完全かつ正確に分かち書きを行いました。

                        分かち書きトークンリスト:
                        $segmentsJson

                        あなたは上記のトークンリストの順番、表記、要素数を「絶対に」維持したまま、各トークンの詳細分析を行ってください。
                        トークンを勝手に結合したり、分割したり、順番を変えたりすることは固く禁じます。

                        出力する JSON の `segments` 配列の要素数は、上記のトークンリストと完全に一致し、各要素の `text` はトークンリストの文字列と完全に一致していなければなりません。
                    """.trimIndent()
                } else {
                    text.ifBlank { "画像内のすべての日本語の単語や品詞、活用を詳細に分析してください。" }
                }

                val promptClauses = if (text.isNotBlank() && text != "画像文法分析") {
                    "分析対象の文: \"$text\"\nこの文の文節（フレーズ）ごとの役割や意味の説明を行ってください。"
                } else {
                    "画像内のすべての日本語の文節構造を詳細に分析してください。"
                }

                val promptGrammar = if (text.isNotBlank() && text != "画像文法分析") {
                    "分析対象の文: \"$text\"\nこの文に含まれるコアとなる文型や特殊文法（受身、使役、敬語）、固定表現、助詞のニュアンスなどを詳細に解説してください。"
                } else {
                    "画像内のすべての日本語の文法表現や固定表現を詳細に分析してください。"
                }

                val (transJson, segJson, clauseJson, grammarJson) = kotlinx.coroutines.coroutineScope {
                    val deferredTrans = async {
                        callLlmApi(
                            PromptManager.SYSTEM_PROMPT_TRANSLATION,
                            promptTrans,
                            imageBase64,
                            mimeType,
                            provider,
                            modelName,
                            effectiveUrl,
                            apiKey
                        )
                    }
                    val deferredSegs = async {
                        callLlmApi(
                            PromptManager.SYSTEM_PROMPT_SEGMENTS,
                            promptSeg,
                            imageBase64,
                            mimeType,
                            provider,
                            modelName,
                            effectiveUrl,
                            apiKey
                        )
                    }
                    val deferredClauses = async {
                        callLlmApi(
                            PromptManager.SYSTEM_PROMPT_CLAUSES,
                            promptClauses,
                            imageBase64,
                            mimeType,
                            provider,
                            modelName,
                            effectiveUrl,
                            apiKey
                        )
                    }
                    val deferredGrammar = async {
                        callLlmApi(
                            PromptManager.SYSTEM_PROMPT_GRAMMAR,
                            promptGrammar,
                            imageBase64,
                            mimeType,
                            provider,
                            modelName,
                            effectiveUrl,
                            apiKey
                        )
                    }

                    listOf(
                        deferredTrans.await(),
                        deferredSegs.await(),
                        deferredClauses.await(),
                        deferredGrammar.await()
                    )
                }

                val transClean = cleanMarkdownJson(transJson)
                val segsClean = cleanMarkdownJson(segJson)
                val clausesClean = cleanMarkdownJson(clauseJson)
                val grammarClean = cleanMarkdownJson(grammarJson)

                val transObj = try { gson.fromJson(transClean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                val segsObj = try { gson.fromJson(segsClean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                val clausesObj = try { gson.fromJson(clausesClean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                val grammarObj = try { gson.fromJson(grammarClean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }

                val combinedResultObj = DetailedAnalysisResult(
                    translation = transObj?.translation,
                    segments = segsObj?.segments,
                    clauses = clausesObj?.clauses,
                    grammarPoints = grammarObj?.grammarPoints
                )

                val mergedResult = gson.toJson(combinedResultObj)

                val currentRecord = analysisDao.getRecordById(recordId)
                if (currentRecord != null) {
                    var updatedText = currentRecord.originalText
                    if (updatedText == "画像文法分析" || updatedText.isBlank()) {
                        val parsed = parseDetailedResult("", mergedResult)
                        if (parsed != null) {
                            val combinedSentence = parsed.segments?.joinToString("") { it.text ?: "" } ?: ""
                            if (combinedSentence.isNotBlank()) {
                                updatedText = combinedSentence
                            }
                        }
                    }
                    analysisDao.update(
                        currentRecord.copy(
                            originalText = updatedText,
                            analysisResult = mergedResult,
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

    private suspend fun callLlmApi(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        provider: String,
        modelName: String,
        effectiveUrl: String,
        apiKey: String
    ): String {
        return when (provider) {
            "DeepSeek", "OpenAI Compatible", "Qwen" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/chat/completions"

                // User message content: plain text or multipart (text + image)
                val userContent: Any = if (imageBase64 != null) {
                    listOf(
                        OpenAiContentPart(type = "text", text = userPrompt),
                        OpenAiContentPart(
                            type = "image_url",
                            image_url = OpenAiImageUrl(url = "data:$mimeType;base64,$imageBase64")
                        )
                    )
                } else {
                    userPrompt
                }

                val request = OpenAiRequest(
                    model = modelName,
                    messages = listOf(
                        // System prompt as a dedicated system-role message for correct instruction priority
                        OpenAiMessage(role = "system", content = systemPrompt),
                        OpenAiMessage(role = "user", content = userContent)
                    )
                )
                val response = llmService.generateOpenAiCompatible(url, "Bearer $apiKey", request)
                response.choices.firstOrNull()?.message?.content ?: throw Exception("No response from model")
            }
            "Gemini", "Vertex AI" -> {
                val cleanBase = effectiveUrl.trimEnd('/')
                val url = "$cleanBase/models/$modelName:generateContent?key=$apiKey"

                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = userPrompt))
                if (imageBase64 != null && mimeType != null) {
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = imageBase64)))
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = parts)),
                    systemInstruction = GeminiContent(role = "user", parts = listOf(GeminiPart(text = systemPrompt)))
                )
                val response = llmService.generateGemini(url, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No response from model")
            }
            else -> throw Exception("Unsupported provider")
        }
    }

    private fun cleanMarkdownJson(rawJson: String): String {
        var cleanJson = rawJson.trim()
        if (cleanJson.startsWith("```")) {
            val firstNewLine = cleanJson.indexOf('\n')
            cleanJson = if (firstNewLine != -1) {
                cleanJson.substring(firstNewLine).trim()
            } else {
                cleanJson.removePrefix("```").trim()
            }
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```").trim()
        }
        return cleanJson
    }

    suspend fun extractTextFromImage(uri: Uri): String {
        return ocrHelper.extractTextFromUri(context, uri)
    }

    override fun onCleared() {
        super.onCleared()
        ocrHelper.close()
    }
}
