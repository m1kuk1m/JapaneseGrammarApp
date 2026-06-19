package com.example.japanesegrammarapp.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    val outputTokens: Int = 0,
    val recordId: Int? = null,
    val stepName: String? = null,
    val attempt: Int? = null,
    val elapsedMs: Long? = null
)

object AppLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _apiLogs = MutableStateFlow<List<ApiDebugLog>>(emptyList())
    val apiLogs: StateFlow<List<ApiDebugLog>> = _apiLogs

    private var appContext: Context? = null
    private val fileLock = Any()
    private lateinit var logScope: CoroutineScope

    val sessionStartTimeMs: Long = System.currentTimeMillis()
    var previousSessionStartTimeMs: Long = 0L

    fun init(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        appContext = app
        logScope = scope
        
        val prefs = app.getSharedPreferences("app_logger_prefs", Context.MODE_PRIVATE)
        previousSessionStartTimeMs = prefs.getLong("last_session_start", 0L)
        prefs.edit().putLong("last_session_start", sessionStartTimeMs).apply()
        
        launchLog {
            synchronized(fileLock) {
                // Load general logs
                val appLogFile = File(app.cacheDir, "exports/app_logs.txt")
                if (appLogFile.exists()) {
                    try {
                        val lines = appLogFile.readLines()
                        _logs.update { current -> (lines + current).takeLast(300) }
                    } catch (e: Exception) {
                        android.util.Log.e("AppLogger", "Failed to load app logs", e)
                    }
                }
                // Load api logs
                val apiLogFile = File(app.cacheDir, "exports/api_logs.json")
                if (apiLogFile.exists()) {
                    try {
                        val json = apiLogFile.readText()
                        val loaded: List<ApiDebugLog> = com.google.gson.Gson()
                            .fromJson(json, Array<ApiDebugLog>::class.java)
                            ?.toList()
                            ?: emptyList()
                        _apiLogs.update { current ->
                            (loaded + current)
                                .distinct()
                                .takeLast(80)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppLogger", "Failed to load api logs", e)
                    }
                }
                
                // Add session start marker after loading old logs
                val time = now()
                val marker = "[$time] D/SYSTEM: --- APP SESSION START ---"
                _logs.update { current -> (current + marker).takeLast(300) }
                writeAppLogToFile(marker)
            }
        }
    }

    fun d(tag: String, message: String) {
        val time = now()
        val safeMessage = message.safeForLog()
        val log = "[$time] D/$tag: $safeMessage"
        android.util.Log.d(tag, safeMessage)
        _logs.update { (it + log).takeLast(300) }
        writeAppLogToFile(log)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val time = now()
        val safeMessage = message.safeForLog()
        val safeStackTrace = throwable?.stackTraceToString()?.safeForLog(12000)
        val log = "[$time] E/$tag: $safeMessage" + (safeStackTrace?.let { "\n$it" } ?: "")
        if (safeStackTrace == null) {
            android.util.Log.e(tag, safeMessage)
        } else {
            android.util.Log.e(tag, "$safeMessage\n$safeStackTrace")
        }
        _logs.update { (it + log).takeLast(300) }
        writeAppLogToFile(log)
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
        outputTokens: Int,
        recordId: Int? = null,
        stepName: String? = null,
        attempt: Int? = null,
        elapsedMs: Long? = null
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
            outputTokens = outputTokens,
            recordId = recordId,
            stepName = stepName,
            attempt = attempt,
            elapsedMs = elapsedMs
        )
        val updatedLogs = appendApiLog(entry)
        d("API_DEBUG", "[$apiTypeLabel] success via $provider/$model, tokens=$consumedTokens, image=$hasImage, record=$recordId, step=$stepName, elapsed=${elapsedMs ?: 0}ms")
        writeApiLogsToFile(updatedLogs)
    }

    fun apiEvent(
        apiTypeLabel: String,
        provider: String,
        model: String,
        status: String,
        hasImage: Boolean,
        userPrompt: String,
        systemPrompt: String,
        message: String,
        recordId: Int? = null,
        stepName: String? = null,
        attempt: Int? = null,
        elapsedMs: Long? = null
    ) {
        val entry = ApiDebugLog(
            time = nowWithMillis(),
            apiTypeLabel = apiTypeLabel,
            provider = provider,
            model = model,
            status = status,
            hasImage = hasImage,
            userPrompt = userPrompt.safeForLog(),
            systemPromptPreview = systemPrompt.safeForLog(1200),
            errorMessage = message.safeForLog(4000),
            recordId = recordId,
            stepName = stepName,
            attempt = attempt,
            elapsedMs = elapsedMs
        )
        val updatedLogs = appendApiLog(entry)
        d("API_DEBUG", "[$apiTypeLabel] $status via $provider/$model, record=$recordId, step=$stepName, attempt=${attempt ?: 0}, elapsed=${elapsedMs ?: 0}ms: $message")
        writeApiLogsToFile(updatedLogs)
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
        rawResponse: String? = null,
        recordId: Int? = null,
        stepName: String? = null,
        attempt: Int? = null,
        elapsedMs: Long? = null
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
            stackTrace = throwable?.stackTraceToString()?.safeForLog(12000),
            recordId = recordId,
            stepName = stepName,
            attempt = attempt,
            elapsedMs = elapsedMs
        )
        val updatedLogs = appendApiLog(entry)
        e("API_DEBUG", "[$apiTypeLabel] failed via $provider/$model, image=$hasImage, record=$recordId, step=$stepName, elapsed=${elapsedMs ?: 0}ms: $message", throwable)
        writeApiLogsToFile(updatedLogs)
    }

    fun clear() {
        _logs.value = emptyList()
        launchLog {
            synchronized(fileLock) {
                appContext?.let { context ->
                    try {
                        val file = File(context.cacheDir, "exports/app_logs.txt")
                        val fileBak = File(context.cacheDir, "exports/app_logs.txt.bak")
                        if (file.exists()) file.delete()
                        if (fileBak.exists()) fileBak.delete()
                    } catch (e: Exception) {
                        android.util.Log.e("AppLogger", "Failed to clear app logs", e)
                    }
                }
            }
        }
    }

    fun clearApiLogs() {
        _apiLogs.value = emptyList()
        launchLog {
            synchronized(fileLock) {
                appContext?.let { context ->
                    try {
                        val file = File(context.cacheDir, "exports/api_logs.json")
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        android.util.Log.e("AppLogger", "Failed to clear api logs", e)
                    }
                }
            }
        }
    }

    fun getLogFileSize(context: Context): String {
        var size = 0L
        synchronized(fileLock) {
            try {
                val appLogFile = File(context.cacheDir, "exports/app_logs.txt")
                val apiLogFile = File(context.cacheDir, "exports/api_logs.json")
                if (appLogFile.exists()) size += appLogFile.length()
                if (apiLogFile.exists()) size += apiLogFile.length()
            } catch (e: Exception) {
                // ignore
            }
        }
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format(Locale.US, "%.2f KB", size / 1024.0)
            else -> String.format(Locale.US, "%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    fun getLogFileUri(context: Context): android.net.Uri? {
        val filename = "app_logs.txt"
        val file = File(context.cacheDir, "exports/$filename")
        synchronized(fileLock) {
            if (!file.exists()) {
                try {
                    file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                    file.createNewFile()
                } catch (e: Exception) {
                    return null
                }
            }
        }
        return try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.japanesegrammarapp.fileprovider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("AppLogger", "Failed to get URI for sharing: $filename", e)
            null
        }
    }

    private fun writeAppLogToFile(log: String) {
        launchLog {
            synchronized(fileLock) {
                appContext?.let { context ->
                    try {
                        val file = File(context.cacheDir, "exports/app_logs.txt")
                        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                        if (file.exists() && file.length() > 2 * 1024 * 1024) { // 2MB limit
                            val backup = File(context.cacheDir, "exports/app_logs.txt.bak")
                            if (backup.exists()) backup.delete()
                            file.renameTo(backup)
                        }
                        file.appendText(log + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("AppLogger", "Failed to write app log to file", e)
                    }
                }
            }
        }
    }

    private fun writeApiLogsToFile(logsList: List<ApiDebugLog>) {
        launchLog {
            synchronized(fileLock) {
                appContext?.let { context ->
                    try {
                        val file = File(context.cacheDir, "exports/api_logs.json")
                        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                        val json = com.google.gson.Gson().toJson(logsList)
                        file.writeText(json)
                    } catch (e: Exception) {
                        android.util.Log.e("AppLogger", "Failed to write api logs to file", e)
                    }
                }
            }
        }
    }

    private fun appendApiLog(entry: ApiDebugLog): List<ApiDebugLog> {
        var updatedLogs: List<ApiDebugLog> = emptyList()
        _apiLogs.update { current ->
            (current + entry).takeLast(80).also { updatedLogs = it }
        }
        return updatedLogs
    }

    private fun now(): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    private fun nowWithMillis(): String = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    private fun launchLog(block: () -> Unit) {
        if (::logScope.isInitialized) {
            logScope.launch { block() }
        }
    }

    private fun String.safeForLog(maxLength: Int = 8000): String {
        val redacted = LogSanitizer.sanitize(this)
        return if (redacted.length > maxLength) redacted.take(maxLength) + "\n...<truncated>" else redacted
    }

    fun logCrashSync(throwable: Throwable) {
        val time = now()
        val safeStackTrace = throwable.stackTraceToString().safeForLog(12000)
        val log = "[$time] F/CRASH: FATAL UNCAUGHT EXCEPTION\n$safeStackTrace"

        android.util.Log.e("CRASH", log)

        synchronized(fileLock) {
            appContext?.let { context ->
                try {
                    val file = File(context.cacheDir, "exports/app_logs.txt")
                    file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                    file.appendText(log + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("AppLogger", "Failed to write crash log synchronously", e)
                }
            }
        }
    }
}
