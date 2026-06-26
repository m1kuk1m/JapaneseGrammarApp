package com.example.japanesegrammarapp.domain.model

import java.net.URI

object EndpointUrlValidator {
    fun isValidHttpUrl(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return false

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
