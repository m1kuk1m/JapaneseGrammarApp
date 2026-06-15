package com.example.japanesegrammarapp.utils

object LogSanitizer {
    private val dataImageRegex = Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+")
    private val bearerRegex = Regex("(?i)Bearer\\s+[A-Za-z0-9._~+\\-/]+=*")
    private val authorizationHeaderRegex = Regex("(?i)(Authorization\\s*[:=]\\s*)(?:Bearer\\s+)?[^\\s,;]+")
    private val queryKeyRegex = Regex("(?i)([?&](?:key|api_key|apikey|access_token|token)=)[^\\s&#]+")
    private val jsonSecretRegex = Regex("(?i)(\"(?:apiKey|api_key|key|token|accessToken|access_token|authorization)\"\\s*:\\s*\")[^\"]+(\")")
    private val assignmentSecretRegex = Regex("(?i)\\b((?:apiKey|api_key|apikey|accessToken|access_token|token)\\s*=\\s*)[^\\s,;&]+")
    private val headerSecretRegex = Regex("(?i)((?:X-Api-Key|api-key)\\s*[:=]\\s*)[^\\s,;]+")

    fun sanitize(value: String): String {
        return value
            .replace(dataImageRegex, "data:image/...;base64,<redacted>")
            .replace(authorizationHeaderRegex) { "${it.groupValues[1]}Bearer <redacted>" }
            .replace(bearerRegex, "Bearer <redacted>")
            .replace(queryKeyRegex) { "${it.groupValues[1]}<redacted>" }
            .replace(jsonSecretRegex) { "${it.groupValues[1]}<redacted>${it.groupValues[2]}" }
            .replace(assignmentSecretRegex) { "${it.groupValues[1]}<redacted>" }
            .replace(headerSecretRegex) { "${it.groupValues[1]}<redacted>" }
    }
}
