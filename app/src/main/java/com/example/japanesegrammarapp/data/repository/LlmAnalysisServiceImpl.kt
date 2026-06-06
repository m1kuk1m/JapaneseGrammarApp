package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.repository.LlmAnalysisService
import com.example.japanesegrammarapp.domain.repository.LlmRepository
import com.example.japanesegrammarapp.domain.repository.LlmApiConfig
import com.example.japanesegrammarapp.domain.repository.LlmResultMetadata
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.TokenizationResult
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.gson.Gson
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmAnalysisServiceImpl @Inject constructor(
    private val llmRepository: LlmRepository,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson
) : LlmAnalysisService {

    private companion object {
        const val DEFAULT_STEP_TIMEOUT_MS = 150_000L
        const val GRAMMAR_STEP_TIMEOUT_MS = 120_000L
    }

    override suspend fun executeTokenizer(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        isOcrMode: Boolean,
        imageTokenizerMode: String,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Pair<TokenizationResult?, LlmResultMetadata> {
        val systemPrompt = when {
            imageBase64 != null && imageTokenizerMode == "repair" -> settingsRepository.getCustomPrompt("prompt_tokenizer_image_repair")
            imageBase64 != null -> settingsRepository.getCustomPrompt("prompt_tokenizer_image")
            isOcrMode -> settingsRepository.getCustomPrompt("prompt_tokenizer_ocr")
            else -> settingsRepository.getCustomPrompt("prompt_tokenizer")
        }

        val userPrompt = when {
            imageBase64 != null && imageTokenizerMode == "repair" -> "画像内の日本語テキストを読み取り、画像が不鮮明な場合は文脈・日本語としての自然さ・濁点/半濁点の有無を総合して、明らかに不合理な読み取りを補正してください。最終的な本文は recognizedText に、分かち書き結果は tokens に出力してください。"
            imageBase64 != null -> "画像内の日本語テキストを原文のまま忠実に認識し、一切修正せず、文字を変更しないでトークン化してください。画像から読み取った原文は recognizedText に、分かち書き結果は tokens に出力してください。"
            isOcrMode -> "分析対象のOCRテキスト: \"$text\"\nこのテキストのOCR誤認識（て・で誤認、濁点脱落など）を自动修正した上で、トークン化し、文字列の配列として出力してください。"
            else -> "分析対象の文: \"$text\"\nこの文をトークン化し、文字列の配列として出力してください。"
        }

        return executeAnalysisStep(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "単語分割",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = TokenizationResult::class.java,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        )
    }

    override suspend fun executeTranslation(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文の自然な中国語訳を出力してください。"
        } else {
            "画像内の日本語テキストを自然な中国語（簡体字）に翻訳してください。"
        }

        return executeAnalysisStep(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_translation"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "翻訳",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        )
    }

    override suspend fun executeClauses(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文の文節（フレーズ）ごとの文法的役割の詳細な解説を行ってください。"
        } else {
            "画像内のすべての日本語の文節構造と文法的役割を詳細に分析してください。"
        }

        return executeAnalysisStep(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_clauses"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "文節解析",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        )
    }

    override suspend fun executeGrammar(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。"
        } else {
            "画像内のすべての日本語の文法表现や固定表現を詳細に分析してください。"
        }

        return executeAnalysisStep(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_grammar"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "文法解説",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = GRAMMAR_STEP_TIMEOUT_MS
        )
    }

    override suspend fun executeSegments(
        text: String,
        tokens: List<String>,
        imageBase64: String?,
        mimeType: String?,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Pair<DetailedAnalysisResult?, LlmResultMetadata> {
        val nonPunctuationTokens = tokens.filter { !isPunctuation(it) }

        // If the sentence contains only punctuation (or is empty), return immediately without calling LLM
        if (nonPunctuationTokens.isEmpty()) {
            val finalSegments = tokens.map { getPunctuationSegment(it) }
            val result = DetailedAnalysisResult(segments = finalSegments)
            return Pair(result, LlmResultMetadata(0, 0, 0))
        }

        val tokensJson = gson.toJson(nonPunctuationTokens)
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nユーザーが提供したトークン配列（記号除く）: $tokensJson\n各トークンの詳細な文法分析を行ってください。"
        } else {
            "画像内の日本語テキストのトークン配列（記号除く）: $tokensJson\n各トークンの詳細な文法分析を行ってください。"
        }

        val (parsed, metadata) = executeAnalysisStep(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_segments"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "詳細文法解析",
            primaryConfig = primaryConfig,
            backupConfig = backupConfig,
            onRetry = onRetry,
            onBackup = onBackup,
            clazz = DetailedAnalysisResult::class.java,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        )

        val returnedSegments = parsed?.segments ?: emptyList()
        var returnedIdx = 0
        val finalSegments = mutableListOf<WordSegment>()

        for (token in tokens) {
            if (isPunctuation(token)) {
                finalSegments.add(getPunctuationSegment(token))
            } else {
                if (returnedIdx < returnedSegments.size) {
                    finalSegments.add(returnedSegments[returnedIdx])
                    returnedIdx++
                } else {
                    finalSegments.add(WordSegment(text = token))
                }
            }
        }

        val optimizedResult = parsed?.copy(segments = finalSegments) ?: DetailedAnalysisResult(segments = finalSegments)
        return Pair(optimizedResult, metadata)
    }



    private suspend fun <T> executeAnalysisStep(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        primaryConfig: LlmApiConfig,
        backupConfig: LlmApiConfig?,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        clazz: Class<T>,
        recordId: Int?,
        stepName: String?,
        timeoutMs: Long
    ): Pair<T?, LlmResultMetadata> {
        val providerLabel = buildString {
            append(primaryConfig.provider)
            if (backupConfig != null) append(" -> ").append(backupConfig.provider)
        }
        val modelLabel = buildString {
            append(primaryConfig.modelName)
            if (backupConfig != null) append(" -> ").append(backupConfig.modelName)
        }
        try {
            val stepStartMs = System.currentTimeMillis()
            val result = try {
                withTimeout(timeoutMs) {
                    llmRepository.executeWithFailover(
                        systemPrompt = systemPrompt,
                        userPrompt = userPrompt,
                        imageBase64 = imageBase64,
                        mimeType = mimeType,
                        apiTypeLabel = apiTypeLabel,
                        primaryConfig = primaryConfig,
                        backupConfig = backupConfig,
                        recordId = recordId,
                        stepName = stepName,
                        onRetry = onRetry,
                        onBackup = onBackup
                    )
                }
            } catch (e: TimeoutCancellationException) {
                val elapsedMs = System.currentTimeMillis() - stepStartMs
                val message = "Step timed out after ${timeoutMs / 1000}s"
                AppLogger.apiEvent(
                    apiTypeLabel = apiTypeLabel,
                    provider = providerLabel,
                    model = modelLabel,
                    status = "TIMEOUT",
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = message,
                    recordId = recordId,
                    stepName = stepName,
                    elapsedMs = elapsedMs
                )
                throw Exception(message, e)
            }
            val clean = cleanMarkdownJson(result.text)
            val parsed = try {
                gson.fromJson(clean, clazz)
            } catch (e: Exception) {
                AppLogger.apiError(
                    apiTypeLabel = apiTypeLabel,
                    provider = providerLabel,
                    model = modelLabel,
                    hasImage = imageBase64 != null,
                    userPrompt = userPrompt,
                    systemPrompt = systemPrompt,
                    message = "JSON parse failed: ${e.localizedMessage}",
                    throwable = e,
                    rawResponse = result.text,
                    recordId = recordId,
                    stepName = stepName,
                    elapsedMs = System.currentTimeMillis() - stepStartMs
                )
                throw e
            }
            AppLogger.apiSuccess(
                apiTypeLabel = apiTypeLabel,
                provider = result.provider ?: providerLabel,
                model = result.modelName ?: modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                rawResponse = result.text,
                parsedPreview = clean,
                consumedTokens = result.consumedTokens,
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens,
                recordId = recordId,
                stepName = stepName,
                elapsedMs = System.currentTimeMillis() - stepStartMs
            )
            return Pair(parsed, LlmResultMetadata(result.consumedTokens, result.inputTokens, result.outputTokens))
        } catch (e: retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string() ?: ""
            val safeBody = if (body.length > 200) body.take(200) + "..." else body
            val message = "HTTP ${e.code()}: ${e.message()}\n$safeBody"
            AppLogger.apiError(
                apiTypeLabel = apiTypeLabel,
                provider = providerLabel,
                model = modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                message = message,
                throwable = e,
                rawResponse = body,
                recordId = recordId,
                stepName = stepName
            )
            throw Exception(message, e)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.apiError(
                apiTypeLabel = apiTypeLabel,
                provider = providerLabel,
                model = modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                message = e.localizedMessage ?: "Unknown LLM step error",
                throwable = e,
                recordId = recordId,
                stepName = stepName
            )
            throw e
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

    private fun isPunctuation(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return true
        val punctuationChars = setOf(
            '。', '、', '・', '？', '！', '「', '」', '『', '』', '（', '）',
            '〔', '〕', '［', '］', '｛', '｝', '〜', '～', '…', '：', '；', '―',
            '【', '】', '《', '》', '〈', '〉',
            '?', '!', '(', ')', '[', ']', '{', '}', ':', ';', ',', '.', '~', '-', '_', '/', '\\', '|', '<', '>', '"', '\''
        )
        if (trimmed.length == 1 && trimmed[0] in punctuationChars) return true
        if (trimmed.matches(Regex("^[wW]+$"))) return true
        return false
    }

    private fun getPunctuationSegment(token: String): WordSegment {
        val trimmed = token.trim()
        val (reading, meaning, role) = when {
            trimmed == "。" -> Triple("くてん", "（句号）", "文の終わりを示す句点。")
            trimmed == "、" -> Triple("とうてん", "（逗号）", "文の区切りを示す読点。")
            trimmed == "・" -> Triple("なかぐろ", "（间隔号）", "名詞の並列や、外来語・外国語の区切りを示す中黒。")
            trimmed == "？" || trimmed == "?" -> Triple("ぎもんふ", "（问号）", "疑問や問いかけを表す疑問符（クエスチョンマーク）。")
            trimmed == "！" || trimmed == "!" -> Triple("かんたんふ", "（感叹号）", "強い感情、驚き、または命令を表す感嘆符。")
            trimmed == "「" -> Triple("かぎかっこ", "（前引号）", "会話、引用、または強調する語句の開始を示す鉤括弧。")
            trimmed == "」" -> Triple("かぎかっこ", "（后引号）", "会話、引用、または強調する語句の終了を示す鉤括弧。")
            trimmed == "『" -> Triple("にじゅうかぎかっこ", "（前双引号）", "書名、作品名、または鉤括弧内での引用の開始を示す二重鉤括弧。")
            trimmed == "』" -> Triple("にじゅうかぎかっこ", "（后双引号）", "書名、作品名、または鉤括弧内での引用の終了を示す二重鉤括弧。")
            trimmed == "（" || trimmed == "(" -> Triple("かっこ", "（前括号）", "注記、補足説明、または読み仮名の開始を示す丸括弧。")
            trimmed == "）" || trimmed == ")" -> Triple("かっこ", "（后括号）", "注記、補足説明、または読み仮名の終了を示す丸括弧。")
            trimmed == "【" -> Triple("すみつきかっこ", "（前方括号）", "見出し、強調、または特別な分類の開始を示す隅付き括弧。")
            trimmed == "】" -> Triple("すみつきかっこ", "（后方括号）", "見出し、強調、または特別な分類の終了を示す隅付き括弧。")
            trimmed == "〔" -> Triple("きっこうかっこ", "（前六角括号）", "引用内の補足、注記、または記号の開始を示す亀甲括弧。")
            trimmed == "〕" -> Triple("きっこうかっこ", "（后六角括号）", "引用内の補足、注記、または記号の終了を示す亀甲括弧。")
            trimmed == "［" || trimmed == "[" -> Triple("かくかっこ", "（前中括号）", "注記、編集上の挿入、またはグループ化の開始を示す角括弧。")
            trimmed == "］" || trimmed == "]" -> Triple("かくかっこ", "（后中括号）", "注記、編集上の挿入、またはグループ化の終了を示す角括弧。")
            trimmed == "｛" || trimmed == "{" -> Triple("なみかっこ", "（前大括号）", "複数の選択肢やグループの開始を示す波括弧。")
            trimmed == "｝" || trimmed == "}" -> Triple("なみかっこ", "（后大括号）", "複数の選択肢やグループの終了を示す波括弧。")
            trimmed == "〜" || trimmed == "～" || trimmed == "~" -> Triple("なみだっしゅ", "（波浪号）", "範囲（〜から〜まで）や起点を示す波ダッシュ。")
            trimmed == "…" || trimmed == "..." -> Triple("さんてんりーだー", "（省略号）", "言葉の省略、余韻、または沈黙を示す三点リーダー。")
            trimmed == "：" || trimmed == ":" -> Triple("ころん", "（冒号）", "説明、例示、または引用の提示を示すコロン。")
            trimmed == "；" || trimmed == ";" -> Triple("せみころん", "（分号）", "文の緊密な関係にある節を区切るセミコロン。")
            trimmed == "―" || trimmed == "—" -> Triple("だっしゅ", "（破折号）", "話の転換、説明の挿入、または余韻を示すダッシュ。")
            trimmed.matches(Regex("^[wW]+$")) -> Triple("わら", "（笑）", "ネットスラングで笑いを表す記号。")
            else -> Triple("きごう", "（符号）", "記号または補助記号。")
        }
        return WordSegment(
            text = token,
            reading = reading,
            partOfSpeech = "補助記号",
            posCategory = "OTHER",
            dictionaryForm = null,
            dictionaryFormReading = null,
            meaning = meaning,
            inflection = null,
            role = role
        )
    }
}
