package com.example.japanesegrammarapp.utils

object ApiLogExportFormatter {
    fun format(
        logs: List<ApiDebugLog>,
        includeFullDebug: Boolean
    ): String {
        return logs.joinToString("\n\n") { log ->
            buildString {
                appendLine("[${log.apiTypeLabel.safe()}] ${log.provider.safe()} - ${log.model.safe()}")
                appendLine("Time: ${log.time.safe()}")
                appendLine("Status: ${log.status.safe()}")
                appendLine("Record: ${log.recordId ?: "-"}")
                appendLine("Step: ${log.stepName?.safe() ?: "-"}")
                appendLine("Attempt: ${log.attempt ?: "-"}")
                appendLine("Elapsed: ${log.elapsedMs ?: "-"}ms")
                appendLine("Image: ${log.hasImage}")
                appendLine("Tokens: ${log.consumedTokens} (in=${log.inputTokens}, out=${log.outputTokens})")
                if (!log.errorMessage.isNullOrBlank()) {
                    appendLine("Error: ${log.errorMessage.safe()}")
                }
                if (includeFullDebug) {
                    appendLine()
                    appendLine("System Prompt Preview:")
                    appendLine(log.systemPromptPreview.safe())
                    appendLine("User Prompt:")
                    appendLine(log.userPrompt.safe())
                    if (!log.rawResponse.isNullOrBlank()) {
                        appendLine("Raw Response:")
                        appendLine(log.rawResponse.safe())
                    }
                    if (!log.parsedPreview.isNullOrBlank()) {
                        appendLine("Parsed Preview:")
                        appendLine(log.parsedPreview.safe())
                    }
                    if (!log.stackTrace.isNullOrBlank()) {
                        appendLine("Stack Trace:")
                        appendLine(log.stackTrace.safe())
                    }
                }
            }.trimEnd()
        }
    }

    private fun String.safe(): String = LogSanitizer.sanitize(this)
}
