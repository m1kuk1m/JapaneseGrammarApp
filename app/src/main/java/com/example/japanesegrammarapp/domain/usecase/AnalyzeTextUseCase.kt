package com.example.japanesegrammarapp.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.japanesegrammarapp.data.AnalysisEvent
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.data.repository.HistoryRepository
import com.example.japanesegrammarapp.data.repository.LlmRepository
import com.example.japanesegrammarapp.data.repository.OcrRepository
import com.example.japanesegrammarapp.data.repository.SettingsRepository
import com.example.japanesegrammarapp.network.DetailedAnalysisResult
import com.example.japanesegrammarapp.network.JapaneseSegmenter
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

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
            originalText = text.ifBlank { "画像文法分析" },
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
            try {
                val isOcrEnabled = settingsRepository.getUseOcr()
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
                    "画像内の日本語テキストを自然な中国語（簡体字）に翻訳してください。"
                }

                val promptSeg = if (preSegments.isNotEmpty()) {
                    val segmentsJson = gson.toJson(preSegments)
                    val expectedSize = preSegments.size
                    """
                        分析対象の文: "$text"

                        【極めて重要な制約条件】
                        あなたは日本語テキストの「分かち書き（セグメンテーション）」を行う必要はありません。すでに以下のように分かち書きされています。

                        分かち書きトークンリスト（全 ${expectedSize} 個のトークン）:
                        $segmentsJson

                        必ず上記のトークンリストの順番、表記、要素数を「絶対に」维持したまま、各トークンの詳細分析を行ってください。
                        出力する JSON の `segments` 配列の要素数は、**必ず正確に ${expectedSize} 個**でなければなりません。各要素の `text` は、上記のトークンリストの各文字列と一字一句完全に一致させてください。トークンを勝手に結合・分割・省略することは固く禁じます。
                    """.trimIndent()
                } else {
                    text.ifBlank { "画像内のすべての日本語の単語や品詞、活用を詳細に分析してください。" }
                }

                val promptClauses = if (text.isNotBlank() && text != "画像文法分析") {
                    "分析対象の文: \"$text\"\nこの文の文節（フレーズ）ごとの文法的役割の詳細な解説を行ってください。"
                } else {
                    "画像内のすべての日本語の文節構造と文法的役割を詳細に分析してください。"
                }

                val promptGrammar = if (text.isNotBlank() && text != "画像文法分析") {
                    if (preSegments.isNotEmpty()) {
                        val segmentsJson = gson.toJson(preSegments)
                        """
                        分析対象の文: "$text"
                        
                        【形態素解析結果（重要：複合助詞・接続形式の見落とし防止のために必ず活用してください）】
                        以下は形態素解析器による分かち書き結果です。この語彙リストを参考として活用し、複合助詞・接続形式・慣用表現を見落とさないようにしてください。
                        $segmentsJson
                        
                        上記の文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。
                        """.trimIndent()
                    } else {
                        "分析対象の文: \"$text\"\nこの文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。"
                    }
                } else {
                    "画像内のすべての日本語の文法表现や固定表現を詳細に分析してください。"
                }

                // Call all 4 APIs concurrently
                val (transJson, segJson, clauseJson, grammarJson) = coroutineScope {
                    val deferredTrans = async {
                        llmRepository.callLlmApi(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_TRANSLATION, promptTrans, imageBase64, mimeType, provider, modelName, baseUrl, apiKey)
                    }
                    val deferredSegs = async {
                        llmRepository.callLlmApi(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_SEGMENTS, promptSeg, imageBase64, mimeType, provider, modelName, baseUrl, apiKey)
                    }
                    val deferredClauses = async {
                        llmRepository.callLlmApi(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_CLAUSES, promptClauses, imageBase64, mimeType, provider, modelName, baseUrl, apiKey)
                    }
                    val deferredGrammar = async {
                        llmRepository.callLlmApi(com.example.japanesegrammarapp.network.PromptManager.SYSTEM_PROMPT_GRAMMAR, promptGrammar, imageBase64, mimeType, provider, modelName, baseUrl, apiKey)
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

                val currentRecord = historyRepository.getRecordById(recordId)
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
                    val updatedRecord = currentRecord.copy(
                        originalText = updatedText,
                        analysisResult = mergedResult,
                        status = "COMPLETED"
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
                e.printStackTrace()
                val errorMsg = when (e) {
                    is retrofit2.HttpException -> {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        "HTTP ${e.code()}: ${e.message()}\n$body"
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
        return try {
            val cleanJson = cleanMarkdownJson(jsonString)
            val initialResult = gson.fromJson(cleanJson, DetailedAnalysisResult::class.java) ?: return null

            if (originalText.isNotBlank() && originalText != "画像文法分析") {
                val expectedTokens = JapaneseSegmenter.segmentAndCombine(originalText)
                if (expectedTokens.isNotEmpty()) {
                    val apiSegments = initialResult.segments.orEmpty()

                    if (apiSegments.size == expectedTokens.size) {
                        val alignedSegments = apiSegments.mapIndexed { idx, segment ->
                            if (segment.text != expectedTokens[idx]) {
                                segment.copy(text = expectedTokens[idx])
                            } else {
                                segment
                            }
                        }
                        initialResult.copy(segments = alignedSegments)
                    } else {
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
}
