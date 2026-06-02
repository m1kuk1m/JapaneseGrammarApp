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
        sb.appendLine("-".repeat(40))
        return sb.toString()
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
