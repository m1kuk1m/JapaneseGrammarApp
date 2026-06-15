package com.example.japanesegrammarapp.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {
    @Test
    fun sanitizeRedactsBearerTokensAndQueryKeys() {
        val raw = "Authorization: Bearer sk-test-secret https://host/models?key=abc123&other=ok"

        val sanitized = LogSanitizer.sanitize(raw)

        assertFalse(sanitized.contains("sk-test-secret"))
        assertFalse(sanitized.contains("abc123"))
        assertTrue(sanitized.contains("Bearer <redacted>"))
        assertTrue(sanitized.contains("key=<redacted>"))
    }

    @Test
    fun sanitizeRedactsJsonSecretsAndImagePayloads() {
        val raw = """
            {"apiKey":"secret-json","authorization":"Bearer nested-secret"}
            data:image/png;base64,AAAA1111BBBB2222
        """.trimIndent()

        val sanitized = LogSanitizer.sanitize(raw)

        assertFalse(sanitized.contains("secret-json"))
        assertFalse(sanitized.contains("nested-secret"))
        assertFalse(sanitized.contains("AAAA1111BBBB2222"))
        assertTrue(sanitized.contains("data:image/...;base64,<redacted>"))
    }
}
