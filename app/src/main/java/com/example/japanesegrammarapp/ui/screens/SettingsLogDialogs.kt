package com.example.japanesegrammarapp.ui.screens

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.utils.ApiDebugLog
import com.example.japanesegrammarapp.utils.ApiLogExportFormatter

@Composable
fun SettingsLogDialogs(
    logs: List<String>,
    apiLogs: List<ApiDebugLog>,
    showLogsDialog: Boolean,
    onDismissLogs: () -> Unit,
    showApiLogsDialog: Boolean,
    onDismissApiLogs: () -> Unit,
    selectedApiLogDetail: ApiDebugLog?,
    onSelectedApiLogDetailChange: (ApiDebugLog?) -> Unit,
    includeFullApiLogExport: Boolean,
    onIncludeFullApiLogExportChange: (Boolean) -> Unit,
    pendingShareLogs: Boolean?,
    onPendingShareLogsChange: (Boolean?) -> Unit,
    pendingCopyApiLogs: List<ApiDebugLog>?,
    onPendingCopyApiLogsChange: (List<ApiDebugLog>?) -> Unit,
    onShareAppLogs: () -> Unit,
    onShareApiLogs: (List<ApiDebugLog>, Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (showLogsDialog) {
        AppLogsDialog(
            logs = logs,
            onDismiss = onDismissLogs,
            onShare = { onPendingShareLogsChange(false) }
        )
    }

    if (showApiLogsDialog) {
        ApiLogsDialog(
            apiLogs = apiLogs,
            includeFullApiLogExport = includeFullApiLogExport,
            onIncludeFullApiLogExportChange = onIncludeFullApiLogExportChange,
            onDismiss = onDismissApiLogs,
            onShareAll = { onPendingShareLogsChange(true) },
            onCopyLogs = { onPendingCopyApiLogsChange(it) },
            onSelectLog = onSelectedApiLogDetailChange
        )
    }

    selectedApiLogDetail?.let { log ->
        ApiLogDetailDialog(
            log = log,
            onDismiss = { onSelectedApiLogDetailChange(null) }
        )
    }

    pendingShareLogs?.let { isApiLog ->
        AlertDialog(
            onDismissRequest = { onPendingShareLogsChange(null) },
            title = { Text(stringResource(R.string.log_export_confirm_title)) },
            text = {
                Text(
                    text = if (isApiLog && includeFullApiLogExport) {
                        stringResource(R.string.api_log_export_full_confirm)
                    } else if (isApiLog) {
                        stringResource(R.string.api_log_export_summary_confirm)
                    } else {
                        stringResource(R.string.app_log_export_confirm)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isApiLog) {
                            onShareApiLogs(apiLogs.reversed(), includeFullApiLogExport)
                        } else {
                            onShareAppLogs()
                        }
                        onPendingShareLogsChange(null)
                    }
                ) {
                    Text(stringResource(R.string.export))
                }
            },
            dismissButton = {
                TextButton(onClick = { onPendingShareLogsChange(null) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingCopyApiLogs?.let { logsToCopy ->
        AlertDialog(
            onDismissRequest = { onPendingCopyApiLogsChange(null) },
            title = { Text(stringResource(R.string.log_copy_confirm_title)) },
            text = {
                Text(
                    text = if (includeFullApiLogExport) {
                        stringResource(R.string.api_log_copy_full_confirm)
                    } else {
                        stringResource(R.string.api_log_copy_summary_confirm)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val copyText = ApiLogExportFormatter.format(logsToCopy, includeFullApiLogExport)
                        clipboardManager.setText(AnnotatedString(copyText))
                        Toast.makeText(
                            context,
                            context.getString(R.string.copy_success_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                        onPendingCopyApiLogsChange(null)
                    }
                ) {
                    Text(stringResource(R.string.copy_logs))
                }
            },
            dismissButton = {
                TextButton(onClick = { onPendingCopyApiLogsChange(null) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
