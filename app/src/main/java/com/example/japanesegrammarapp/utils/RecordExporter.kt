package com.example.japanesegrammarapp.utils

import android.content.Context
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordExporter {

    fun buildRecordExportText(context: Context, record: AnalysisDomainRecord, index: Int? = null): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        if (index != null) {
            sb.appendLine(context.getString(R.string.export_record_number, index))
        }
        sb.appendLine(context.getString(R.string.export_record_time, sdf.format(Date(record.timestamp))))
        sb.appendLine(context.getString(R.string.export_record_model, record.modelUsed))
        val statusStr = when (record.status) {
            AnalysisStatus.PENDING -> context.getString(R.string.history_status_pending)
            AnalysisStatus.FAILED -> context.getString(R.string.history_status_error)
            else -> context.getString(R.string.completed)
        }
        sb.appendLine(context.getString(R.string.export_record_status, statusStr))
        sb.appendLine("-".repeat(40))
        sb.appendLine(context.getString(R.string.export_original_text_section))
        sb.appendLine(record.originalText.ifBlank { context.getString(R.string.export_image_analysis_fallback) })
        if (record.status == AnalysisStatus.COMPLETED && !record.analysisResult.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine(context.getString(R.string.export_result_section))
            sb.appendLine(record.analysisResult)
        } else if (record.status == AnalysisStatus.FAILED) {
            sb.appendLine()
            sb.appendLine(context.getString(R.string.export_error_section))
            sb.appendLine(record.errorMessage ?: context.getString(R.string.unknown_error))
        }
        try {
            val recordJson = com.google.gson.Gson().toJson(record)
            val recordBase64 = android.util.Base64.encodeToString(recordJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            sb.appendLine("##METADATA:$recordBase64##")
        } catch (e: Exception) {
            // Ignore serialisation errors to ensure the human readable format is exported
        }
        sb.appendLine("-".repeat(40))
        return sb.toString()
    }

    fun parseRecordsFromExportText(content: String): List<AnalysisDomainRecord> {
        val metadataRegex = Regex("##METADATA:([A-Za-z0-9+/=]+)##")
        val matches = metadataRegex.findAll(content).toList()
        if (matches.isNotEmpty()) {
            return matches.map { match ->
                val base64 = match.groupValues[1]
                val json = String(android.util.Base64.decode(base64, android.util.Base64.DEFAULT), Charsets.UTF_8)
                val record = com.google.gson.Gson().fromJson(json, AnalysisDomainRecord::class.java)
                record.copy(id = 0) // Reset ID for SQLite auto-generation
            }
        }

        // Fallback text parser for legacy exports (without metadata)
        val records = mutableListOf<AnalysisDomainRecord>()
        val blocks = content.split(Regex("-{10,}"))
        for (block in blocks) {
            if (block.isBlank()) continue

            var timestamp = System.currentTimeMillis()
            var modelUsed = "Unknown"
            var status = AnalysisStatus.COMPLETED

            var readingOriginalText = false
            var readingResult = false
            var readingError = false

            val originalTextBuilder = StringBuilder()
            val resultBuilder = StringBuilder()
            val errorBuilder = StringBuilder()

            val sdfFormats = listOf(
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            )

            for (line in block.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Check section headers
                if (trimmed == "【原文】" || trimmed == "[Original Text]") {
                    readingOriginalText = true
                    readingResult = false
                    readingError = false
                    continue
                }
                if (trimmed == "【分析结果】" || trimmed == "【分析結果】" || trimmed == "[Analysis Result]") {
                    readingOriginalText = false
                    readingResult = true
                    readingError = false
                    continue
                }
                if (trimmed == "【错误内容】" || trimmed == "【エラー内容】" || trimmed == "[Error Details]") {
                    readingOriginalText = false
                    readingResult = false
                    readingError = true
                    continue
                }

                // If not in a content block, try parsing field metadata headers
                if (!readingOriginalText && !readingResult && !readingError) {
                    if (trimmed.startsWith("时间:") || trimmed.startsWith("Time:") || trimmed.startsWith("日時:") || trimmed.startsWith("Date:")) {
                        val dateStr = trimmed.substringAfter(":").trim()
                        for (sdf in sdfFormats) {
                            try {
                                val date = sdf.parse(dateStr)
                                if (date != null) {
                                    timestamp = date.time
                                    break
                                }
                            } catch (e: Exception) {
                                // Try next format
                            }
                        }
                        continue
                    }
                    if (trimmed.startsWith("模型:") || trimmed.startsWith("Model:") || trimmed.startsWith("モデル:")) {
                        modelUsed = trimmed.substringAfter(":").trim()
                        continue
                    }
                    if (trimmed.startsWith("状态:") || trimmed.startsWith("Status:") || trimmed.startsWith("状態:")) {
                        val statusStr = trimmed.substringAfter(":").trim()
                        status = when {
                            statusStr.contains("错误") || statusStr.contains("error") || statusStr.contains("FAILED") -> AnalysisStatus.FAILED
                            statusStr.contains("分析中") || statusStr.contains("pending") || statusStr.contains("PENDING") -> AnalysisStatus.PENDING
                            else -> AnalysisStatus.COMPLETED
                        }
                        continue
                    }
                }

                // Add content lines
                if (readingOriginalText) {
                    if (originalTextBuilder.isNotEmpty()) originalTextBuilder.append("\n")
                    originalTextBuilder.append(line)
                } else if (readingResult) {
                    if (resultBuilder.isNotEmpty()) resultBuilder.append("\n")
                    resultBuilder.append(line)
                } else if (readingError) {
                    if (errorBuilder.isNotEmpty()) errorBuilder.append("\n")
                    errorBuilder.append(line)
                }
            }

            val originalText = originalTextBuilder.toString().trim()
            if (originalText.isNotEmpty() && originalText != "（图片分析）" && originalText != "(Image Analysis)") {
                val analysisResult = resultBuilder.toString().trim().ifEmpty { null }
                val errorMessage = errorBuilder.toString().trim().ifEmpty { null }
                records.add(
                    AnalysisDomainRecord(
                        id = 0,
                        originalText = originalText,
                        imageUri = null,
                        analysisResult = analysisResult,
                        timestamp = timestamp,
                        modelUsed = modelUsed,
                        status = status,
                        errorMessage = errorMessage,
                        consumedTokens = 0,
                        inputTokens = 0,
                        outputTokens = 0
                    )
                )
            }
        }
        return records
    }

    fun buildAllHistoryExportText(context: Context, records: List<AnalysisDomainRecord>): String {
        val sb = StringBuilder()

        sb.appendLine(context.getString(R.string.export_header_title))
        sb.appendLine(context.getString(R.string.export_header_time, SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())))
        sb.appendLine(context.getString(R.string.export_header_count, records.size))
        sb.appendLine("=".repeat(60))
        sb.appendLine()
        records.forEachIndexed { index, record ->
            sb.appendLine(buildRecordExportText(context, record, index + 1))
            sb.appendLine()
        }
        return sb.toString()
    }

    fun exportRecordToFile(context: Context, record: AnalysisDomainRecord, filename: String): android.net.Uri {
        val content = buildRecordExportText(context, record)
        val exportDir = java.io.File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = java.io.File(exportDir, filename)
        file.writeText(content)
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.japanesegrammarapp.fileprovider",
            file
        )
    }

    fun exportAllHistoryToFile(context: Context, records: List<AnalysisDomainRecord>, filename: String): android.net.Uri {
        val content = buildAllHistoryExportText(context, records)
        val exportDir = java.io.File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = java.io.File(exportDir, filename)
        file.writeText(content)
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.japanesegrammarapp.fileprovider",
            file
        )
    }
}
