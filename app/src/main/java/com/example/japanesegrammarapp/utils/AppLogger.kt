package com.example.japanesegrammarapp.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiDebugLog(
    val id: Long = System.currentTimeMillis(),
    val time: String,
    val apiTypeLabel: String,
    val provider: String,
    val model: String,
    val status: String,
    val hasImage: Boolean,
    val userPrompt: String,
    val systemPromptPreview: String,
    val rawResponse: String? = null,
    val parsedPreview: String? = null,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
    val consumedTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0
)

object AppLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _apiLogs = MutableStateFlow<List<ApiDebugLog>>(emptyList())
    val apiLogs: StateFlow<List<ApiDebugLog>> = _apiLogs

    fun d(tag: String, message: String) {
        val time = now()
        val log = "[$time] D/$tag: $message"
        android.util.Log.d(tag, message)
        _logs.update { (it + log).takeLast(300) }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val time = now()
        val log = "[$time] E/$tag: $message" + (throwable?.let { "\n${it.stackTraceToString()}" } ?: "")
        android.util.Log.e(tag, message, throwable)
        _logs.update { (it + log).takeLast(300) }
    }

    fun apiSuccess(
        apiTypeLabel: String,
        provider: String,
        model: String,
        hasImage: Boolean,
        userPrompt: String,
        systemPrompt: String,
        rawResponse: String,
        parsedPreview: String?,
        consumedTokens: Int,
        inputTokens: Int,
        outputTokens: Int
    ) {
        val entry = ApiDebugLog(
            time = nowWithMillis(),
            apiTypeLabel = apiTypeLabel,
            provider = provider,
            model = model,
            status = "SUCCESS",
            hasImage = hasImage,
            userPrompt = userPrompt.safeForLog(),
            systemPromptPreview = systemPrompt.safeForLog(1200),
            rawResponse = rawResponse.safeForLog(12000),
            parsedPreview = parsedPreview?.safeForLog(4000),
            consumedTokens = consumedTokens,
            inputTokens = inputTokens,
            outputTokens = outputTokens
        )
        _apiLogs.update { (it + entry).takeLast(80) }
        d("API_DEBUG", "[$apiTypeLabel] success via $provider/$model, tokens=$consumedTokens, image=$hasImage")
    }

    fun apiError(
        apiTypeLabel: String,
        provider: String,
        model: String,
        hasImage: Boolean,
        userPrompt: String,
        systemPrompt: String,
        message: String,
        throwable: Throwable? = null,
        rawResponse: String? = null
    ) {
        val entry = ApiDebugLog(
            time = nowWithMillis(),
            apiTypeLabel = apiTypeLabel,
            provider = provider,
            model = model,
            status = "ERROR",
            hasImage = hasImage,
            userPrompt = userPrompt.safeForLog(),
            systemPromptPreview = systemPrompt.safeForLog(1200),
            rawResponse = rawResponse?.safeForLog(12000),
            errorMessage = message.safeForLog(4000),
            stackTrace = throwable?.stackTraceToString()?.safeForLog(12000)
        )
        _apiLogs.update { (it + entry).takeLast(80) }
        e("API_DEBUG", "[$apiTypeLabel] failed via $provider/$model, image=$hasImage: $message", throwable)
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun clearApiLogs() {
        _apiLogs.value = emptyList()
    }

    private fun now(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private fun nowWithMillis(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    private fun String.safeForLog(maxLength: Int = 8000): String {
        val redacted = replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "data:image/...;base64,<redacted>")
        return if (redacted.length > maxLength) redacted.take(maxLength) + "\n...<truncated>" else redacted
    }
}
