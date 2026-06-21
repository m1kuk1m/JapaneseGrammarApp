import re

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Import ConflictStrategy & ImportResult
content = content.replace('import com.example.japanesegrammarapp.ui.BookmarkViewModel\n',
'''import com.example.japanesegrammarapp.ui.BookmarkViewModel
import com.example.japanesegrammarapp.domain.model.ConflictStrategy
import com.example.japanesegrammarapp.domain.model.ImportResult
import com.example.japanesegrammarapp.domain.model.ExportFormat
''')

# 2. Add state variables
state_vars = '''    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    
    var showConflictDialog by remember { mutableStateOf(false) }
    var pendingImportParams by remember { mutableStateOf<ImportParams?>(null) }
    
    var showImportSummaryDialog by remember { mutableStateOf(false) }
    var importSummaryResult by remember { mutableStateOf<ImportResult?>(null) }'''
content = re.sub(r'    var showExportDialog.*?var pendingImportUri by remember \{ mutableStateOf<Uri\?>\(null\) \}', state_vars, content, flags=re.DOTALL)

# 3. Export confirm
export_confirm_old = '''            onConfirm = { words, sentences ->
                showExportDialog = false
                viewModel.exportAndShare(context, words, sentences)
            }'''
export_confirm_new = '''            onConfirm = { words, sentences, grammar, format ->
                showExportDialog = false
                viewModel.exportAndShare(context, format, words, sentences, grammar)
            }'''
content = content.replace(export_confirm_old, export_confirm_new)

# 4. Import dialog block
import_block_old = '''    if (showImportDialog) {
        BookmarkSelectionDialog(
            titleResId = R.string.import_options_title,
            confirmResId = R.string.import_history,
            onDismiss = {
                showImportDialog = false
                pendingImportUri = null
            },
            onConfirm = { words, sentences ->
                showImportDialog = false
                val uri = pendingImportUri
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val count = viewModel.importFromUri(uri, words, sentences)
                            val msg = if (count >= 0) {
                                context.getString(R.string.import_success_msg, count)
                            } else {
                                context.getString(R.string.import_failed_msg)
                            }
                            snackbarHostState.showSnackbar(msg)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.import_error_msg, e.localizedMessage ?: "")
                            )
                        }
                    }
                }
                pendingImportUri = null
            }
        )
    }'''

import_block_new = '''    if (showImportDialog) {
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
                            val hasConflicts = viewModel.checkConflictsFromUri(uri, format, words, sentences, grammarPoints)
                            if (hasConflicts) {
                                pendingImportParams = ImportParams(uri, format, words, sentences, grammarPoints)
                                showConflictDialog = true
                            } else {
                                val result = viewModel.importFromUri(uri, format, words, sentences, grammarPoints, ConflictStrategy.SKIP)
                                if (result != null) {
                                    importSummaryResult = result
                                    showImportSummaryDialog = true
                                } else {
                                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
                                }
                                pendingImportUri = null
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.import_error_msg, e.localizedMessage ?: "")
                            )
                            pendingImportUri = null
                        }
                    }
                } else {
                    pendingImportUri = null
                }
            }
        )
    }

    if (showConflictDialog) {
        ConflictResolutionDialog(
            onDismiss = {
                showConflictDialog = false
                pendingImportParams = null
                pendingImportUri = null
            },
            onSkip = {
                showConflictDialog = false
                pendingImportParams?.let { params ->
                    coroutineScope.launch {
                        val result = viewModel.importFromUri(params.uri, params.format, params.includeWords, params.includeSentences, params.includeGrammarPoints, ConflictStrategy.SKIP)
                        if (result != null) {
                            importSummaryResult = result
                            showImportSummaryDialog = true
                        } else {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
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
                        val result = viewModel.importFromUri(params.uri, params.format, params.includeWords, params.includeSentences, params.includeGrammarPoints, ConflictStrategy.OVERWRITE)
                        if (result != null) {
                            importSummaryResult = result
                            showImportSummaryDialog = true
                        } else {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_failed_msg))
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
    }'''

content = content.replace(import_block_old, import_block_new)

# Add ImportParams data class at the end
content += '''
data class ImportParams(
    val uri: Uri,
    val format: ExportFormat,
    val includeWords: Boolean,
    val includeSentences: Boolean,
    val includeGrammarPoints: Boolean
)
'''

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

