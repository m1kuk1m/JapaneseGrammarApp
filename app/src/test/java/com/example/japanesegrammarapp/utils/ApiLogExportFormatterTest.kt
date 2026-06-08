package com.example.japanesegrammarapp.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiLogExportFormatterTest {
    @Test
    fun formatSummaryOmitsPromptResponseAndStackTrace() {
        val output = ApiLogExportFormatter.format(listOf(sampleLog()), includeFullDebug = false)

        assertTrue(output.contains("[Grammar] Gemini - gemini-test"))
        assertTrue(output.contains("Status: ERROR"))
        assertTrue(output.contains("Tokens: 7 (in=3, out=4)"))
        assertTrue(output.contains("Error: sanitized error"))
        assertFalse(output.contains("system prompt"))
        assertFalse(output.contains("user prompt"))
        assertFalse(output.contains("raw response"))
        assertFalse(output.contains("parsed preview"))
        assertFalse(output.contains("stack trace"))
    }

    @Test
    fun formatFullDebugIncludesPromptResponseAndStackTrace() {
        val output = ApiLogExportFormatter.format(listOf(sampleLog()), includeFullDebug = true)

        assertTrue(output.contains("System Prompt Preview:"))
        assertTrue(output.contains("system prompt"))
        assertTrue(output.contains("User Prompt:"))
        assertTrue(output.contains("user prompt"))
        assertTrue(output.contains("Raw Response:"))
        assertTrue(output.contains("raw response"))
        assertTrue(output.contains("Parsed Preview:"))
        assertTrue(output.contains("parsed preview"))
        assertTrue(output.contains("Stack Trace:"))
        assertTrue(output.contains("stack trace"))
    }

    @Test
    fun formatSanitizesSecretsEvenWhenFullDebugIsIncluded() {
        val output = ApiLogExportFormatter.format(
            listOf(
                sampleLog().copy(
                    provider = "Authorization: Bearer provider-secret",
                    userPrompt = "prompt with https://host/path?key=query-secret",
                    systemPromptPreview = """{"api_key":"system-secret"}""",
                    rawResponse = "Bearer response-secret",
                    parsedPreview = "token=parsed-secret",
                    errorMessage = "apiKey=error-secret",
                    stackTrace = "Authorization: Bearer stack-secret"
                )
            ),
            includeFullDebug = true
        )

        assertFalse(output.contains("provider-secret"))
        assertFalse(output.contains("query-secret"))
        assertFalse(output.contains("system-secret"))
        assertFalse(output.contains("response-secret"))
        assertFalse(output.contains("parsed-secret"))
        assertFalse(output.contains("error-secret"))
        assertFalse(output.contains("stack-secret"))
        assertTrue(output.contains("<redacted>"))
    }

    private fun sampleLog(): ApiDebugLog {
        return ApiDebugLog(
            time = "12:00:00.000",
            apiTypeLabel = "Grammar",
            provider = "Gemini",
            model = "gemini-test",
            status = "ERROR",
            hasImage = true,
            userPrompt = "user prompt",
            systemPromptPreview = "system prompt",
            rawResponse = "raw response",
            parsedPreview = "parsed preview",
            errorMessage = "sanitized error",
            stackTrace = "stack trace",
            consumedTokens = 7,
            inputTokens = 3,
            outputTokens = 4,
            recordId = 9,
            stepName = "TRANSLATION",
            attempt = 2,
            elapsedMs = 1234
        )
    }
}
