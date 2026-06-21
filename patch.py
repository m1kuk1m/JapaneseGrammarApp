import sys

with open(r'app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt', 'r', encoding='utf-8') as f:
    code = f.read()

start_str = '    if (showImportDialog) {'
idx = code.find(start_str)

if idx != -1:
    code = code[:idx]

import_new = """    if (showImportDialog) {
        BookmarkSelectionDialog(
            titleResId = R.string.import_options_title,
            confirmResId = R.string.import_history,
            onDismiss = {
                showImportDialog = false
                pendingImportUri = null
            },
            onConfirm = { words, sentences, grammarPoints, format ->
                showImportDialog = false
                val uri = pendingImportUri
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val result = viewModel.importFromUri(uri, format, words, sentences, grammarPoints)
                            if (result != null) {
                                importSummaryResult = result
                                showImportSummaryDialog = true
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                            }
                        } catch (e: Exception) {
                            if (e.message == "CONFLICT") {
                                pendingImportParams = ImportParams(uri, format, words, sentences, grammarPoints)
                                showConflictDialog = true
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.import_error_msg, e.localizedMessage ?: "")
                                )
                            }
                        }
                    }
                } else {
                    pendingImportUri = null
                }
            }
        )
    }
    
    if (showConflictDialog) {
        com.example.japanesegrammarapp.ui.screens.components.ConflictResolutionDialog(
            onDismiss = {
                showConflictDialog = false
                pendingImportParams = null
                pendingImportUri = null
            },
            onSkip = {
                showConflictDialog = false
                pendingImportParams?.let { params ->
                    coroutineScope.launch {
                        try {
                            val result = viewModel.importFromUri(params.uri, params.format, params.includeWords, params.includeSentences, params.includeGrammarPoints, com.example.japanesegrammarapp.domain.model.ConflictStrategy.SKIP)
                            if (result != null) {
                                importSummaryResult = result
                                showImportSummaryDialog = true
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_error_msg, e.localizedMessage ?: ""))
                        }
                        pendingImportUri = null
                        pendingImportParams = null
                    }
                }
            },
            onOverwrite = {
                showConflictDialog = false
                pendingImportParams?.let { params ->
                    coroutineScope.launch {
                        try {
                            val result = viewModel.importFromUri(params.uri, params.format, params.includeWords, params.includeSentences, params.includeGrammarPoints, com.example.japanesegrammarapp.domain.model.ConflictStrategy.OVERWRITE)
                            if (result != null) {
                                importSummaryResult = result
                                showImportSummaryDialog = true
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_error_msg, e.localizedMessage ?: ""))
                        }
                        pendingImportUri = null
                        pendingImportParams = null
                    }
                }
            }
        )
    }

    if (showImportSummaryDialog && importSummaryResult != null) {
        ImportSummaryDialog(
            result = importSummaryResult!!,
            onDismiss = {
                showImportSummaryDialog = false
                importSummaryResult = null
                pendingImportUri = null
            }
        )
    }
}

data class ImportParams(
    val uri: android.net.Uri,
    val format: com.example.japanesegrammarapp.domain.model.ExportFormat,
    val includeWords: Boolean,
    val includeSentences: Boolean,
    val includeGrammarPoints: Boolean
)

@Composable
fun ImportSummaryDialog(
    result: com.example.japanesegrammarapp.domain.model.ImportResult,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_title),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_success, result.successCount))
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_skipped, result.skippedCount))
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_failed, result.failedCount))
                if (result.failureReasons.isNotEmpty()) {
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        text = androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.import_summary_failure_reasons),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    androidx.compose.foundation.lazy.LazyColumn(modifier = androidx.compose.ui.Modifier.heightIn(max = 120.dp)) {
                        items(result.failureReasons.size) { index ->
                            androidx.compose.material3.Text("- ${result.failureReasons[index]}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.example.japanesegrammarapp.R.string.ok))
            }
        }
    )
}
"""

code += import_new

with open(r'app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt', 'w', encoding='utf-8') as f:
    f.write(code)
