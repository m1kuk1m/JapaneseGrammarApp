package com.example.japanesegrammarapp.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.japanesegrammarapp.data.AnalysisEvent
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.repository.HistoryRepository
import com.example.japanesegrammarapp.data.repository.LlmRepository
import com.example.japanesegrammarapp.data.repository.LlmResult
import com.example.japanesegrammarapp.data.repository.OcrRepository
import com.example.japanesegrammarapp.data.repository.SettingsRepository
import com.example.japanesegrammarapp.network.DetailedAnalysisResult
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class AnalysisProgress(
    val tokenizerCompleted: Boolean = false,
    val segmentsCompleted: Boolean = false,
    val clausesCompleted: Boolean = false,
    val translationCompleted: Boolean = false,
    val grammarCompleted: Boolean = false
)

@Singleton
class AnalyzeTextUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val llmRepository: LlmRepository,
    private val ocrRepository: OcrRepository,
    private val gson: Gson
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private val parsedCache = ConcurrentHashMap<String, DetailedAnalysisResult>()

    private val _progressFlow = MutableStateFlow<Map<Int, AnalysisProgress>>(emptyMap())
    val progressFlow: StateFlow<Map<Int, AnalysisProgress>> = _progressFlow.asStateFlow()

    suspend fun execute(
        text: String,
        imageUri: Uri?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ): Int {
        if (text.isBlank() && imageUri == null) {
            throw IllegalArgumentException("Please enter text or capture an image.")
        }
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Missing API Key.")
        }

        val record = AnalysisRecord(
            originalText = text.ifBlank { "" },
            imageUri = imageUri?.toString(),
            analysisResult = null,
            modelUsed = "$provider: $modelName",
            status = "PENDING"
        )
        val recordId = historyRepository.insertRecord(record).toInt()

        val job = launchBackgroundAnalysis(recordId, text, imageUri, provider, modelName, baseUrl, apiKey)
        activeJobs[recordId] = job
        job.invokeOnCompletion { activeJobs.remove(recordId) }

        return recordId
    }

    suspend fun executeRetry(
        recordId: Int,
        text: String,
        imageUri: Uri?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ) {
        val job = launchBackgroundAnalysis(recordId, text, imageUri, provider, modelName, baseUrl, apiKey)
        activeJobs[recordId] = job
        job.invokeOnCompletion { activeJobs.remove(recordId) }
    }

    fun cancel(recordId: Int) {
        val job = activeJobs.remove(recordId)
        job?.cancel()
        repositoryScope.launch {
            val record = historyRepository.getRecordById(recordId)
            if (record != null) {
                historyRepository.deleteRecord(record)
            }
        }
    }

    fun close() {
        repositoryScope.cancel()
    }

    private fun launchBackgroundAnalysis(
        recordId: Int,
        text: String,
        imageUri: Uri?,
        provider: String,
        modelName: String,
        baseUrl: String,
        apiKey: String
    ): Job {
        return repositoryScope.launch(Dispatchers.IO) {
            _progressFlow.update { it + (recordId to AnalysisProgress()) }
            var partialResult = DetailedAnalysisResult()
            val partialResultMutex = kotlinx.coroutines.sync.Mutex()

            val dbWriteChannel = Channel<DetailedAnalysisResult>(Channel.UNLIMITED)
            val dbWriterJob = launch {
                for (resultToSave in dbWriteChannel) {
                    try {
                        val currentRecord = historyRepository.getRecordById(recordId)
                        if (currentRecord != null) {
                            val mergedResult = gson.toJson(resultToSave)
                            historyRepository.updateRecord(
                                currentRecord.copy(
                                    analysisResult = mergedResult,
                                    consumedTokens = resultToSave.consumedTokens,
                                    inputTokens = resultToSave.inputTokens,
                                    outputTokens = resultToSave.outputTokens
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            fun savePartial(snapshot: DetailedAnalysisResult) {
                dbWriteChannel.trySend(snapshot)
            }

            try {
                val isOcrEnabled = settingsRepository.getUseOcr()
                var isOcrMode = isOcrEnabled && imageUri != null

                var imageBase64: String? = null
                var mimeType: String? = "image/jpeg"
                if (!isOcrEnabled && imageUri != null) {
                    val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        imageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                    }
                }

                var ocrText = text
                if (isOcrMode) {
                    if (ocrText.isBlank()) {
                        try {
                            ocrText = ocrRepository.extractTextFromImage(imageUri!!)
                        } catch (e: Exception) {
                            ocrText = ""
                        }
                    }

                    if (ocrText.isBlank()) {
                        // Fallback to Vision Mode if local OCR yielded empty text
                        isOcrMode = false
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "ローカルOCRで文字が検出されなかったため、Visionモードにフォールバックします。",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Re-load image bytes for Vision Mode fallback
                        val bytes = context.contentResolver.openInputStream(imageUri!!)?.use { it.readBytes() }
                        if (bytes != null) {
                            imageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                        }
                    }
                }

                if (isOcrMode) {
                    // ==========================================
                    // OCR Mode Flow (Sequential Tokenizer first)
                    // ==========================================
                    val promptTokenizer = "分析対象のOCRテキスト: \"$ocrText\"\nこのテキストのOCR誤認識（て・で誤認、濁点脱落など）を自動修正した上で、トークン化し、文字列の配列として出力してください。"

                    // 1. Execute Tokenizer first (acts as OCR correction & spelling grammar checker)
                    // No image is transmitted in OCR mode (imageBase64 = null)
                    val tokenRes = llmRepository.executeWithFailover(
                        com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_TOKENIZER_OCR,
                        promptTokenizer,
                        null,
                        null,
                        "単語分割"
                    )
                    val cleanTokenJson = cleanMarkdownJson(tokenRes.text)
                    val tokenObj = try { gson.fromJson(cleanTokenJson, com.example.japanesegrammarapp.network.TokenizationResult::class.java) } catch (e: Exception) { null }
                    val tokens = tokenObj?.tokens ?: emptyList()
                    val correctedText = tokenObj?.correctedText

                    var effectiveText = ocrText
                    if (!correctedText.isNullOrBlank() && correctedText != ocrText) {
                        effectiveText = correctedText
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "文章の誤りを自動修正しました。",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Emit skeleton tokens to UI immediately
                    val skeletonSegments = tokens.map { com.example.japanesegrammarapp.network.WordSegment(text = it) }
                    if (effectiveText != text) {
                        val currentRecord = historyRepository.getRecordById(recordId)
                        if (currentRecord != null) {
                            historyRepository.updateRecord(currentRecord.copy(originalText = effectiveText))
                        }
                    }
                    val snapshot = partialResultMutex.withLock {
                        if (skeletonSegments.isNotEmpty()) {
                            partialResult = partialResult.copy(segments = skeletonSegments)
                        }
                        partialResult.consumedTokens += tokenRes.consumedTokens
                        partialResult.inputTokens += tokenRes.inputTokens
                        partialResult.outputTokens += tokenRes.outputTokens
                        partialResult
                    }
                    savePartial(snapshot)

                    _progressFlow.update { map ->
                        val current = map[recordId] ?: AnalysisProgress()
                        map + (recordId to current.copy(tokenizerCompleted = true))
                    }

                    // Prepare prompts using the effective corrected text
                    val promptTrans = "分析対象の文: \"$effectiveText\"\nこの文の自然な中国語訳を出力してください。"
                    val promptClauses = "分析対象の文: \"$effectiveText\"\nこの文の文節（フレーズ）ごとの文法的役割の詳細な解説を行ってください。"
                    val promptGrammar = "分析対象の文: \"$effectiveText\"\nこの文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。"

                    val tokensJsonString = gson.toJson(tokens)
                    val promptSeg = "分析対象の文: \"$effectiveText\"\nユーザーが提供したトークン配列: $tokensJsonString\n各トークンの詳細な文法分析を行ってください。"

                    // Execute remaining 4 calls concurrently (using supervisorScope to prevent cascading cancellation)
                    supervisorScope {
                        // Translation
                        launch {
                            try {
                                val res = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_TRANSLATION, promptTrans, null, null, "翻訳")
                                val clean = cleanMarkdownJson(res.text)
                                val obj = try { gson.fromJson(clean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    partialResult = partialResult.copy(translation = obj?.translation)
                                    partialResult.consumedTokens += res.consumedTokens
                                    partialResult.inputTokens += res.inputTokens
                                    partialResult.outputTokens += res.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(translationCompleted = true))
                                }
                            }
                        }

                        // Clauses
                        launch {
                            try {
                                val res = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_CLAUSES, promptClauses, null, null, "文節解析")
                                val clean = cleanMarkdownJson(res.text)
                                val obj = try { gson.fromJson(clean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    partialResult = partialResult.copy(clauses = obj?.clauses)
                                    partialResult.consumedTokens += res.consumedTokens
                                    partialResult.inputTokens += res.inputTokens
                                    partialResult.outputTokens += res.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(clausesCompleted = true))
                                }
                            }
                        }

                        // Grammar
                        launch {
                            try {
                                val res = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_GRAMMAR, promptGrammar, null, null, "文法解説")
                                val clean = cleanMarkdownJson(res.text)
                                val obj = try { gson.fromJson(clean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    partialResult = partialResult.copy(grammarPoints = obj?.grammarPoints)
                                    partialResult.consumedTokens += res.consumedTokens
                                    partialResult.inputTokens += res.inputTokens
                                    partialResult.outputTokens += res.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(grammarCompleted = true))
                                }
                            }
                        }

                        // Segments (detailed segmentation analysis)
                        launch {
                            try {
                                val segRes = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_SEGMENTS, promptSeg, null, null, "詳細文法解析")
                                val cleanSegJson = cleanMarkdownJson(segRes.text)
                                val segObj = try { gson.fromJson(cleanSegJson, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    if (segObj?.segments != null) {
                                        partialResult = partialResult.copy(segments = segObj.segments)
                                    }
                                    partialResult.consumedTokens += segRes.consumedTokens
                                    partialResult.inputTokens += segRes.inputTokens
                                    partialResult.outputTokens += segRes.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(segmentsCompleted = true))
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // Non-OCR Mode Flow (Tokenizer, Translation, Clauses, Grammar in parallel. Segments runs after Tokenizer.)
                    // ==========================================
                    val promptTokenizer = if (text.isNotBlank()) {
                        "分析対象の文: \"$text\"\nこの文をトークン化し、文字列の配列として出力してください。"
                    } else {
                        "画像内の日本語テキストをトークン化し、文字列の配列として出力してください。"
                    }

                    val promptTrans = if (text.isNotBlank()) {
                        "分析対象の文: \"$text\"\nこの文の自然な中国語訳を出力してください。"
                    } else {
                        "画像内の日本語テキストを自然な中国語（簡体字）に翻訳してください。"
                    }

                    val promptClauses = if (text.isNotBlank()) {
                        "分析対象の文: \"$text\"\nこの文の文節（フレーズ）ごとの文法的役割の詳細な解説を行ってください。"
                    } else {
                        "画像内のすべての日本語の文節構造と文法的役割を詳細に分析してください。"
                    }

                    val promptGrammar = if (text.isNotBlank()) {
                        "分析対象の文: \"$text\"\nこの文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。"
                    } else {
                        "画像内のすべての日本語の文法表现や固定表現を詳細に分析してください。"
                    }

                    supervisorScope {
                        // 1. Translation
                        launch {
                            try {
                                val res = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_TRANSLATION, promptTrans, imageBase64, mimeType, "翻訳")
                                val clean = cleanMarkdownJson(res.text)
                                val obj = try { gson.fromJson(clean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    partialResult = partialResult.copy(translation = obj?.translation)
                                    partialResult.consumedTokens += res.consumedTokens
                                    partialResult.inputTokens += res.inputTokens
                                    partialResult.outputTokens += res.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(translationCompleted = true))
                                }
                            }
                        }

                        // 2. Clauses
                        launch {
                            try {
                                val res = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_CLAUSES, promptClauses, imageBase64, mimeType, "文節解析")
                                val clean = cleanMarkdownJson(res.text)
                                val obj = try { gson.fromJson(clean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    partialResult = partialResult.copy(clauses = obj?.clauses)
                                    partialResult.consumedTokens += res.consumedTokens
                                    partialResult.inputTokens += res.inputTokens
                                    partialResult.outputTokens += res.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(clausesCompleted = true))
                                }
                            }
                        }

                        // 3. Grammar
                        launch {
                            try {
                                val res = llmRepository.executeWithFailover(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_GRAMMAR, promptGrammar, imageBase64, mimeType, "文法解説")
                                val clean = cleanMarkdownJson(res.text)
                                val obj = try { gson.fromJson(clean, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }
                                val snapshot = partialResultMutex.withLock {
                                    partialResult = partialResult.copy(grammarPoints = obj?.grammarPoints)
                                    partialResult.consumedTokens += res.consumedTokens
                                    partialResult.inputTokens += res.inputTokens
                                    partialResult.outputTokens += res.outputTokens
                                    partialResult
                                }
                                savePartial(snapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(grammarCompleted = true))
                                }
                            }
                        }

                        // 4. Tokenizer & Segments Sequence
                        launch {
                            try {
                                // 4a. Execute Tokenizer first
                                val tokenRes = llmRepository.executeWithFailover(
                                    com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_TOKENIZER,
                                    promptTokenizer,
                                    imageBase64,
                                    mimeType,
                                    "単語分割"
                                )
                                val cleanTokenJson = cleanMarkdownJson(tokenRes.text)
                                val tokenObj = try { gson.fromJson(cleanTokenJson, com.example.japanesegrammarapp.network.TokenizationResult::class.java) } catch (e: Exception) { null }
                                val tokens = tokenObj?.tokens ?: emptyList()

                                val skeletonSegments = tokens.map { com.example.japanesegrammarapp.network.WordSegment(text = it) }
                                val tokenSnapshot = partialResultMutex.withLock {
                                    if (skeletonSegments.isNotEmpty()) {
                                        partialResult = partialResult.copy(segments = skeletonSegments)
                                    }
                                    partialResult.consumedTokens += tokenRes.consumedTokens
                                    partialResult.inputTokens += tokenRes.inputTokens
                                    partialResult.outputTokens += tokenRes.outputTokens
                                    partialResult
                                }
                                savePartial(tokenSnapshot)

                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(tokenizerCompleted = true))
                                }

                                // 4b. Execute detailed segments analysis using the retrieved tokens
                                val tokensJsonString = gson.toJson(tokens)
                                val promptSeg = if (text.isNotBlank()) {
                                    "分析対象の文: \"$text\"\nユーザーが提供したトークン配列: $tokensJsonString\n各トークンの詳細な文法分析を行ってください。"
                                } else {
                                    "画像内の日本語テキストのトークン配列: $tokensJsonString\n各トークンの詳細な文法分析を行ってください。"
                                }

                                val segRes = llmRepository.executeWithFailover(
                                    com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_SEGMENTS,
                                    promptSeg,
                                    imageBase64,
                                    mimeType,
                                    "詳細文法解析"
                                )
                                val cleanSegJson = cleanMarkdownJson(segRes.text)
                                val segObj = try { gson.fromJson(cleanSegJson, DetailedAnalysisResult::class.java) } catch (e: Exception) { null }

                                val segmentSnapshot = partialResultMutex.withLock {
                                    if (segObj?.segments != null) {
                                        partialResult = partialResult.copy(segments = segObj.segments)
                                    }
                                    partialResult.consumedTokens += segRes.consumedTokens
                                    partialResult.inputTokens += segRes.inputTokens
                                    partialResult.outputTokens += segRes.outputTokens
                                    partialResult
                                }
                                savePartial(segmentSnapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                _progressFlow.update { map ->
                                    val current = map[recordId] ?: AnalysisProgress()
                                    map + (recordId to current.copy(segmentsCompleted = true))
                                }
                            }
                        }
                    }
                }

                dbWriteChannel.close()
                dbWriterJob.join()

                val currentRecord = historyRepository.getRecordById(recordId)
                if (currentRecord != null) {
                    var updatedText = currentRecord.originalText
                    if (updatedText.isBlank()) {
                        val combinedSentence = partialResult.segments?.joinToString("") { it.text ?: "" } ?: ""
                        if (combinedSentence.isNotBlank()) {
                            updatedText = combinedSentence
                        }
                    }
                    val finalResultJson = gson.toJson(partialResult)
                    val updatedRecord = currentRecord.copy(
                        originalText = updatedText,
                        analysisResult = finalResultJson,
                        status = "COMPLETED",
                        consumedTokens = partialResult.consumedTokens,
                        inputTokens = partialResult.inputTokens,
                        outputTokens = partialResult.outputTokens
                    )
                    historyRepository.updateRecord(updatedRecord)

                    val displayMsg = if (updatedText.length > 10) {
                        "「${updatedText.take(10)}...」の分析が完了しました"
                    } else {
                        "「${updatedText}」の分析が完了しました"
                    }
                    historyRepository.emitEvent(AnalysisEvent.TaskCompleted(recordId, displayMsg))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                dbWriteChannel.close()
                dbWriterJob.join()
                val errorMsg = when (e) {
                    is retrofit2.HttpException -> {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        val safeBody = if (body.length > 200) body.take(200) + "..." else body
                        "HTTP ${e.code()}: ${e.message()}\n$safeBody"
                    }
                    else -> e.localizedMessage ?: "Unknown network error"
                }
                val currentRecord = historyRepository.getRecordById(recordId)
                if (currentRecord != null) {
                    historyRepository.updateRecord(
                        currentRecord.copy(
                            status = "FAILED",
                            errorMessage = errorMsg
                        )
                    )
                }
            } finally {
                dbWriteChannel.close()
                _progressFlow.update { it - recordId }
            }
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

    fun parseDetailedResult(originalText: String, jsonString: String?): DetailedAnalysisResult? {
        if (jsonString.isNullOrBlank()) return null
        val cached = parsedCache[jsonString]
        if (cached != null) return cached
        return try {
            val cleanJson = cleanMarkdownJson(jsonString)
            val parsed = gson.fromJson(cleanJson, DetailedAnalysisResult::class.java)
            if (parsed != null) {
                if (parsedCache.size > 50) {
                    parsedCache.clear()
                }
                parsedCache[jsonString] = parsed
            }
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
