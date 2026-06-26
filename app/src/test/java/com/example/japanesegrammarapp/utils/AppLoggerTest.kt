package com.example.japanesegrammarapp.utils

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLoggerTest {
    @Before
    fun setUp() {
        AppLogger.clear()
        AppLogger.clearApiLogs()
    }

    @After
    fun tearDown() {
        AppLogger.clear()
        AppLogger.clearApiLogs()
    }

    @Test
    fun apiSuccessSanitizesStoredPromptAndResponseFields() {
        AppLogger.apiSuccess(
            apiTypeLabel = "Tokenizer",
            provider = "Gemini",
            model = "gemini-test",
            hasImage = false,
            userPrompt = "Authorization: Bearer user-secret https://host/path?key=query-secret",
            systemPrompt = """{"api_key":"system-secret"}""",
            rawResponse = """{"access_token":"response-secret"}""",
            parsedPreview = "Bearer parsed-secret",
            consumedTokens = 1,
            inputTokens = 2,
            outputTokens = 3
        )

        val stored = AppLogger.apiLogs.value.single()
        val combined = listOf(
            stored.userPrompt,
            stored.systemPromptPreview,
            stored.rawResponse,
            stored.parsedPreview
        ).joinToString("\n")

        assertFalse(combined.contains("user-secret"))
        assertFalse(combined.contains("query-secret"))
        assertFalse(combined.contains("system-secret"))
        assertFalse(combined.contains("response-secret"))
        assertFalse(combined.contains("parsed-secret"))
        assertTrue(combined.contains("<redacted>"))
    }

    @Test
    fun apiErrorSanitizesMessageResponseAndStackTrace() {
        AppLogger.apiError(
            apiTypeLabel = "Grammar",
            provider = "OpenAI Compatible",
            model = "gpt-test",
            hasImage = false,
            userPrompt = "plain prompt",
            systemPrompt = "plain system",
            message = "failed with apiKey=message-secret",
            throwable = IllegalStateException("Authorization: Bearer throwable-secret"),
            rawResponse = "https://host/path?key=response-query-secret"
        )

        val stored = AppLogger.apiLogs.value.single()
        val combined = listOf(
            stored.errorMessage,
            stored.rawResponse,
            stored.stackTrace
        ).joinToString("\n")

        assertFalse(combined.contains("message-secret"))
        assertFalse(combined.contains("throwable-secret"))
        assertFalse(combined.contains("response-query-secret"))
        assertTrue(combined.contains("<redacted>"))
    }

    @Test
    fun debugLogSanitizesStoredGeneralMessage() {
        AppLogger.d("TEST", "Authorization: Bearer debug-secret")

        val stored = AppLogger.logs.value.single()

        assertFalse(stored.contains("debug-secret"))
        assertTrue(stored.contains("Bearer <redacted>"))
    }

    @Test
    fun selectCurrentReportLogsStartsFromPreviousSessionMarker() {
        val selected = AppLogger.selectCurrentReportLogs(
            listOf(
                "[01-01 10:00:00] D/SYSTEM: --- APP SESSION START ---",
                "[01-01 10:00:01] D/OLD: old",
                "[01-01 11:00:00] D/SYSTEM: --- APP SESSION START ---",
                "[01-01 11:00:01] E/CRASH: crash",
                "[01-01 12:00:00] D/SYSTEM: --- APP SESSION START ---",
                "[01-01 12:00:01] D/NEW: new"
            )
        )

        assertFalse(selected.any { it.contains("D/OLD") })
        assertTrue(selected.first().contains("--- APP SESSION START ---"))
        assertTrue(selected.any { it.contains("E/CRASH") })
        assertTrue(selected.any { it.contains("D/NEW") })
    }

    @Test
    fun selectCurrentReportLogsKeepsAllLogsWithoutSessionMarker() {
        val logs = listOf(
            "[01-01 11:00:01] E/CRASH: crash",
            "[01-01 12:00:01] D/NEW: new"
        )

        val selected = AppLogger.selectCurrentReportLogs(logs)

        assertTrue(selected == logs)
    }
}
