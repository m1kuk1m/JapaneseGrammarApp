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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Flow<Pair<TokenizationResult?, LlmResultMetadata>> {
        val systemPrompt = when {
            imageBase64 != null && imageTokenizerMode == "repair" -> settingsRepository.getCustomPrompt("prompt_tokenizer_image_repair")
            imageBase64 != null -> settingsRepository.getCustomPrompt("prompt_tokenizer_image")
            isOcrMode -> settingsRepository.getCustomPrompt("prompt_tokenizer_ocr")
            else -> settingsRepository.getCustomPrompt("prompt_tokenizer")
        }

        val userPrompt = when {
            imageBase64 != null && imageTokenizerMode == "repair" -> "画像内の日本語テキストを視覚情報優先で読み取り、不鮮明な場合も文脈は類似字形候補を選ぶ補助に限定してください。意味・自然さ・頻度・安全性を理由に一般語へ置き換えず、濁点/半濁点/長音符/小書き文字は画像上の痕跡を優先してください。テキストで出力してください。"
            imageBase64 != null -> "画像内の日本語テキストを原文のまま忠実に認識し、一切修正せず、文字を変更しないでトークン化してください。テキストで出力してください。"
            isOcrMode -> "分析対象のOCRテキスト: \"$text\"\nこのテキストのOCR誤認識（て・で誤認、濁点脱落など）を自动修正した上で、トークン化し、出力してください。"
            else -> "分析対象の文: \"$text\"\nこの文をトークン化し、出力してください。"
        }

        return executeAnalysisStepStreaming(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "単語分割",
            primaryConfigs = primaryConfigs,
            backupConfigs = backupConfigs,
            onRetry = onRetry,
            onBackup = onBackup,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        ) { accumulated ->
            val lines = accumulated.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                TokenizationResult()
            } else {
                if (imageBase64 != null || isOcrMode) {
                    val firstLine = lines.firstOrNull() ?: ""
                    val tokens = if (lines.size > 1) lines.drop(1) else emptyList()
                    if (isOcrMode) {
                        TokenizationResult(correctedText = firstLine, tokens = tokens)
                    } else {
                        TokenizationResult(recognizedText = firstLine, tokens = tokens)
                    }
                } else {
                    TokenizationResult(tokens = lines)
                }
            }
        }
    }

    override suspend fun executeTranslation(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文の自然な中国語訳を出力してください。"
        } else {
            "画像内の日本語テキストを自然な中国語（簡体字）に翻訳してください。"
        }

        return executeAnalysisStepStreaming(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_translation"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "翻訳",
            primaryConfigs = primaryConfigs,
            backupConfigs = backupConfigs,
            onRetry = onRetry,
            onBackup = onBackup,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        ) { accumulated ->
            DetailedAnalysisResult(translation = accumulated.trim())
        }
    }

    override suspend fun executeClauses(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文の文節（フレーズ）ごとの文法的役割の詳細な解説を行ってください。"
        } else {
            "画像内のすべての日本語の文節構造と文法的役割を詳細に分析してください。"
        }

        return executeAnalysisStepStreaming(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_clauses"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "文節解析",
            primaryConfigs = primaryConfigs,
            backupConfigs = backupConfigs,
            onRetry = onRetry,
            onBackup = onBackup,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        ) { accumulated ->
            val lines = accumulated.lines().map { it.trim() }.filter { it.isNotBlank() }
            val clauses = lines.mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 4) {
                    com.example.japanesegrammarapp.domain.model.SentenceClause(
                        index = parts[0].toIntOrNull(),
                        role = parts[1],
                        text = parts[2],
                        explanation = parts.drop(3).joinToString("|")
                    )
                } else null
            }
            DetailedAnalysisResult(clauses = clauses)
        }
    }

    override suspend fun executeGrammar(
        text: String,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> {
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nこの文に含まれる最も重要かつ難度の高い文法項目・慣用表現を厳選して解説してください。"
        } else {
            "画像内のすべての日本語の文法表现や固定表現を詳細に分析してください。"
        }

        return executeAnalysisStepStreaming(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_grammar"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "文法解説",
            primaryConfigs = primaryConfigs,
            backupConfigs = backupConfigs,
            onRetry = onRetry,
            onBackup = onBackup,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = GRAMMAR_STEP_TIMEOUT_MS
        ) { accumulated ->
            val lines = accumulated.lines().map { it.trim() }.filter { it.isNotBlank() }
            val points = lines.mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    com.example.japanesegrammarapp.domain.model.DetailedGrammarPoint(
                        pattern = parts[0],
                        explanation = parts.drop(1).joinToString("|")
                    )
                } else null
            }
            DetailedAnalysisResult(grammarPoints = points)
        }
    }

    override suspend fun executeSegments(
        text: String,
        tokens: List<String>,
        imageBase64: String?,
        mimeType: String?,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?
    ): Flow<Pair<DetailedAnalysisResult?, LlmResultMetadata>> = flow {
        val nonPunctuationTokens = tokens.filter { !isPunctuation(it) }

        if (nonPunctuationTokens.isEmpty()) {
            val finalSegments = tokens.map { getPunctuationSegment(it) }
            val result = DetailedAnalysisResult(segments = finalSegments)
            emit(Pair(result, LlmResultMetadata(0, 0, 0)))
            return@flow
        }

        val tokensStr = nonPunctuationTokens.joinToString(", ")
        val userPrompt = if (text.isNotBlank()) {
            "分析対象の文: \"$text\"\nユーザーが提供したトークン配列（記号除く）: $tokensStr\n各トークンの詳細な文法分析を行ってください。"
        } else {
            "画像内の日本語テキストのトークン配列（記号除く）: $tokensStr\n各トークンの詳細な文法分析を行ってください。"
        }

        executeAnalysisStepStreaming(
            systemPrompt = settingsRepository.getCustomPrompt("prompt_segments"),
            userPrompt = userPrompt,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            apiTypeLabel = "詳細文法解析",
            primaryConfigs = primaryConfigs,
            backupConfigs = backupConfigs,
            onRetry = onRetry,
            onBackup = onBackup,
            recordId = recordId,
            stepName = stepName,
            timeoutMs = DEFAULT_STEP_TIMEOUT_MS
        ) { accumulated ->
            val lines = accumulated.lines().map { it.trim() }.filter { it.isNotBlank() }
            val segments = lines.mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 8) {
                    WordSegment(
                        text = parts[0].trim(),
                        reading = parts[1].trim().takeIf { it.isNotBlank() && it != "null" },
                        meaning = parts[2].trim().takeIf { it.isNotBlank() && it != "null" },
                        partOfSpeech = parts[3].trim().takeIf { it.isNotBlank() && it != "null" },
                        dictionaryForm = parts[4].trim().takeIf { it.isNotBlank() && it != "null" },
                        dictionaryFormReading = parts[5].trim().takeIf { it.isNotBlank() && it != "null" },
                        inflection = parts[6].trim().takeIf { it.isNotBlank() && it != "null" },
                        role = parts.drop(7).joinToString("|").trim().takeIf { it.isNotBlank() && it != "null" }
                    )
                } else if (parts.size >= 5) {
                    val remainingParts = parts.drop(4).map { it.trim() }.filter { it.isNotBlank() && it != "null" }
                    WordSegment(
                        text = parts[0].trim(),
                        reading = parts[1].trim().takeIf { it.isNotBlank() && it != "null" },
                        meaning = parts[2].trim().takeIf { it.isNotBlank() && it != "null" },
                        partOfSpeech = parts[3].trim().takeIf { it.isNotBlank() && it != "null" },
                        role = remainingParts.joinToString(" | ").takeIf { it.isNotBlank() }
                    )
                } else null
            }

            val returnedSegments = segments
            var returnedIdx = 0
            val finalSegments = mutableListOf<WordSegment>()

            for (token in tokens) {
                if (token.isBlank()) continue
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
            DetailedAnalysisResult(segments = finalSegments)
        }.collect {
            emit(it)
        }
    }

    private fun <T> executeAnalysisStepStreaming(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        mimeType: String?,
        apiTypeLabel: String,
        primaryConfigs: List<LlmApiConfig>,
        backupConfigs: List<LlmApiConfig>,
        onRetry: (attempt: Int) -> Unit,
        onBackup: (backupProvider: String) -> Unit,
        recordId: Int?,
        stepName: String?,
        timeoutMs: Long,
        parser: (String) -> T?
    ): Flow<Pair<T?, LlmResultMetadata>> = flow {
        var providerLabel = buildString {
            append(primaryConfigs.firstOrNull()?.provider ?: "Main API")
            backupConfigs.firstOrNull()?.provider?.let { append(" -> ").append(it) }
        }
        var modelLabel = buildString {
            append(primaryConfigs.firstOrNull()?.modelName ?: "Unknown")
            backupConfigs.firstOrNull()?.modelName?.let { append(" -> ").append(it) }
        }
        try {
            val stepStartMs = System.currentTimeMillis()
            var accumulatedText = ""
            var currentMetadata = LlmResultMetadata(0, 0, 0)
            
            withTimeout(timeoutMs) {
                llmRepository.executeWithStreaming(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    apiTypeLabel = apiTypeLabel,
                    primaryConfigs = primaryConfigs,
                    backupConfigs = backupConfigs,
                    recordId = recordId,
                    stepName = stepName,
                    onRetry = onRetry,
                    onBackup = onBackup
                ).collect { event ->
                    when (event) {
                        is com.example.japanesegrammarapp.domain.model.LlmStreamEvent.Chunk -> {
                            accumulatedText += event.text
                            val parsed = parser(accumulatedText)
                            emit(Pair(parsed, currentMetadata))
                        }
                        is com.example.japanesegrammarapp.domain.model.LlmStreamEvent.Metadata -> {
                            providerLabel = event.provider
                            modelLabel = event.modelName
                            currentMetadata = event.usage
                            val parsed = parser(accumulatedText)
                            emit(Pair(parsed, currentMetadata))
                        }
                    }
                }
            }
            AppLogger.apiSuccess(
                apiTypeLabel = apiTypeLabel,
                provider = providerLabel,
                model = modelLabel,
                hasImage = imageBase64 != null,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                rawResponse = accumulatedText,
                parsedPreview = accumulatedText.take(100).replace("\n", " "),
                consumedTokens = currentMetadata.consumedTokens,
                inputTokens = currentMetadata.inputTokens,
                outputTokens = currentMetadata.outputTokens,
                recordId = recordId,
                stepName = stepName,
                elapsedMs = System.currentTimeMillis() - stepStartMs
            )
        } catch (e: TimeoutCancellationException) {
            val elapsedMs = System.currentTimeMillis() - System.currentTimeMillis() // just 0 for timeout 
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

    private fun isJapaneseChar(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
               block == Character.UnicodeBlock.HIRAGANA ||
               block == Character.UnicodeBlock.KATAKANA
    }

    private fun isPunctuation(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return true
        val punctuationChars = setOf(
            '。', '、', '・', '？', '！', '「', '」', '『', '』', '（', '）',
            '〔', '〕', '［', '］', '｛', '｝', '〜', '～', '…', '：', '；', '―',
            '【', '】', '《', '》', '〈', '〉', '〝', '〟',
            '?', '!', '(', ')', '[', ']', '{', '}', ':', ';', ',', '.', '~', '-', '_', '/', '\\', '|', '<', '>', '"', '\''
        )
        if (trimmed.all { it in punctuationChars }) return true
        if (trimmed.matches(Regex("^[wW]+$"))) return true
        
        return trimmed.all { c ->
            if (c.isLetterOrDigit()) return@all false
            val block = Character.UnicodeBlock.of(c)
            if (isJapaneseChar(c)) return@all false
            
            val type = Character.getType(c).toByte()
            type == Character.START_PUNCTUATION ||
            type == Character.END_PUNCTUATION ||
            type == Character.OTHER_PUNCTUATION ||
            type == Character.CONNECTOR_PUNCTUATION ||
            type == Character.DASH_PUNCTUATION ||
            type == Character.INITIAL_QUOTE_PUNCTUATION ||
            type == Character.FINAL_QUOTE_PUNCTUATION ||
            type == Character.MATH_SYMBOL ||
            type == Character.CURRENCY_SYMBOL ||
            type == Character.MODIFIER_SYMBOL ||
            type == Character.OTHER_SYMBOL ||
            block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
        }
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
            trimmed == "〝" -> Triple("かぎかっこ", "（前双引号）", "引用や強調を示す二重引用符の開始（ノの字形ダブルクォーテーション）。")
            trimmed == "〟" -> Triple("かぎかっこ", "（后双引号）", "引用や強調を示す二重引用符の終了（ノの字形ダブルクォーテーション）。")
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
            trimmed.isEllipsisLike() -> Triple("さんてんりーだー", "（省略号）", "言葉の省略、余韻、または沈黙を示す三点リーダー。")
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

    private fun String.isEllipsisLike(): Boolean {
        return isNotBlank() && all { it == '.' || it == '…' }
    }
}
