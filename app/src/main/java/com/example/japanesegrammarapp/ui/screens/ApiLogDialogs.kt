package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.utils.ApiDebugLog
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.DateRange

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ApiLogsDialog(
    apiLogs: List<ApiDebugLog>,
    includeFullApiLogExport: Boolean,
    onIncludeFullApiLogExportChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onShareAll: () -> Unit,
    onCopyLogs: (List<ApiDebugLog>) -> Unit,
    onSelectLog: (ApiDebugLog) -> Unit,
    selectedDateOverride: String? = null,
    onSelectedDateChange: ((String?) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") }
    var internalSelectedDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedDate = selectedDateOverride ?: internalSelectedDate
    val setSelectedDate = onSelectedDateChange ?: { internalSelectedDate = it }

    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val filteredApiLogs = remember(apiLogs, searchQuery, filterStatus, selectedDate) {
        apiLogs.filter { log ->
            val matchesQuery = searchQuery.isBlank() ||
                log.provider.contains(searchQuery, ignoreCase = true) ||
                log.model.contains(searchQuery, ignoreCase = true) ||
                log.apiTypeLabel.contains(searchQuery, ignoreCase = true) ||
                log.userPrompt.contains(searchQuery, ignoreCase = true) ||
                (log.rawResponse?.contains(searchQuery, ignoreCase = true) ?: false)

            val matchesStatus = filterStatus == "ALL" || log.status == filterStatus
            val matchesDate = if (selectedDate != null) {
                val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                sdf.format(Date(log.id)) == selectedDate
            } else true
            
            matchesQuery && matchesStatus && matchesDate
        }.reversed()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                        setSelectedDate(sdf.format(Date(millis)))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.api_debug_logs_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShareAll,
                                enabled = apiLogs.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.api_log_share_all),
                                    tint = sumiInk
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = sumiInk
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.log_search_hint), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        textStyle = TextStyle(fontSize = 12.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedDate != null,
                            onClick = {
                                if (selectedDate != null) {
                                    setSelectedDate(null)
                                } else {
                                    showDatePicker = true
                                }
                            },
                            label = { Text(selectedDate ?: "Date", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (selectedDate != null) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = onPrimaryColor,
                                selectedLeadingIconColor = onPrimaryColor,
                                selectedTrailingIconColor = onPrimaryColor
                            )
                        )

                        val statuses = listOf("ALL", "START", "SUCCESS", "RETRY", "BACKUP", "TIMEOUT", "ERROR")
                        statuses.forEach { stat ->
                            FilterChip(
                                selected = filterStatus == stat,
                                onClick = { filterStatus = stat },
                                label = { Text(apiStatusLabel(stat), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = onPrimaryColor
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Divider(color = sumiInk.copy(alpha = 0.1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            apiLogs.isEmpty() -> {
                                Text(
                                    text = stringResource(R.string.api_debug_empty),
                                    fontSize = 13.sp,
                                    color = sumiInk.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                            filteredApiLogs.isEmpty() -> {
                                Text(
                                    text = stringResource(R.string.api_debug_no_filter_results),
                                    fontSize = 13.sp,
                                    color = sumiInk.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(filteredApiLogs) { log ->
                                        ApiLogSummaryCard(
                                            log = log,
                                            sumiInk = sumiInk,
                                            onClick = { onSelectLog(log) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Divider(color = sumiInk.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeFullApiLogExport,
                                onCheckedChange = onIncludeFullApiLogExportChange
                            )
                            Text(
                                text = stringResource(R.string.api_log_include_full_debug),
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = sumiInk.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { AppLogger.clearApiLogs() },
                                enabled = apiLogs.isNotEmpty()
                            ) {
                                Text(stringResource(R.string.clear_api_debug_logs), color = Color.Red, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { 
                                    val copyTarget = if (selectedDate != null || searchQuery.isNotBlank() || filterStatus != "ALL") {
                                        filteredApiLogs
                                    } else {
                                        filteredApiLogs.filter { it.id >= AppLogger.sessionStartTimeMs }
                                    }
                                    onCopyLogs(copyTarget)
                                },
                                enabled = filteredApiLogs.isNotEmpty()
                            ) {
                                Text(stringResource(R.string.copy_logs), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiLogSummaryCard(
    log: ApiDebugLog,
    sumiInk: Color,
    onClick: () -> Unit
) {
    val preview = remember(log) { apiLogPreview(log) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[${log.apiTypeLabel}] ${log.provider}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = sumiInk
                )
                Text(
                    text = apiStatusLabel(log.status),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = apiStatusColor(log.status)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.model,
                    fontSize = 10.sp,
                    color = sumiInk.copy(alpha = 0.7f)
                )
                Text(
                    text = log.time,
                    fontSize = 9.sp,
                    color = sumiInk.copy(alpha = 0.4f)
                )
            }
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = sumiInk.copy(alpha = 0.78f)
                )
            }
            Text(
                text = stringResource(
                    R.string.api_debug_meta,
                    log.hasImage.toString(),
                    log.consumedTokens,
                    log.inputTokens,
                    log.outputTokens
                ),
                fontSize = 9.sp,
                color = sumiInk.copy(alpha = 0.5f)
            )
            if (log.recordId != null || !log.stepName.isNullOrBlank() || log.attempt != null || log.elapsedMs != null) {
                Text(
                    text = stringResource(
                        R.string.api_log_summary_context,
                        log.recordId?.toString() ?: stringResource(R.string.api_log_unknown_value),
                        log.stepName ?: stringResource(R.string.api_log_unknown_value),
                        log.attempt?.toString() ?: stringResource(R.string.api_log_unknown_value),
                        log.elapsedMs?.toString() ?: stringResource(R.string.api_log_unknown_value)
                    ),
                    fontSize = 9.sp,
                    color = sumiInk.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun apiLogPreview(log: ApiDebugLog): String {
    val source = log.parsedPreview
        ?: log.rawResponse
        ?: log.errorMessage
        ?: ""
    val compact = source
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    return if (compact.length > 260) compact.take(260) + "..." else compact
}

@Composable
fun ApiLogDetailDialog(
    log: ApiDebugLog,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val formattedResponse = remember(log.rawResponse) {
        if (log.rawResponse == null) {
            ""
        } else {
            try {
                val jsonElement = JsonParser.parseString(log.rawResponse)
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                log.rawResponse
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.api_details_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SelectionContainer {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ApiLogMetadataCard(log = log, sumiInk = sumiInk)
                            ApiLogPromptSection(log = log, context = context, sumiInk = sumiInk)
                            if (log.status == "SUCCESS") {
                                ApiLogResponseSection(
                                    formattedResponse = formattedResponse,
                                    context = context,
                                    sumiInk = sumiInk
                                )
                            }
                            if (log.status == "ERROR" || log.status == "TIMEOUT") {
                                ApiLogErrorSection(log = log, context = context)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiLogMetadataCard(log: ApiDebugLog, sumiInk: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.api_details_metadata), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sumiInk)
            Divider(color = sumiInk.copy(alpha = 0.05f))
            Text(stringResource(R.string.api_log_time, log.time), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            Text(stringResource(R.string.api_log_type, log.apiTypeLabel), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            Text(stringResource(R.string.api_log_provider, log.provider), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            Text(stringResource(R.string.api_log_model, log.model), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            Text(stringResource(R.string.api_log_status, apiStatusLabel(log.status)), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = apiStatusColor(log.status))
            Text(stringResource(R.string.api_log_tokens, log.consumedTokens, log.inputTokens, log.outputTokens), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            Text(stringResource(R.string.api_log_has_image, log.hasImage), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            if (log.recordId != null || !log.stepName.isNullOrBlank() || log.attempt != null || log.elapsedMs != null) {
                val unknown = stringResource(R.string.api_log_unknown_value)
                Text(stringResource(R.string.api_log_record, log.recordId?.toString() ?: unknown), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
                Text(stringResource(R.string.api_log_step, log.stepName ?: unknown), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
                Text(stringResource(R.string.api_log_attempt, log.attempt?.toString() ?: unknown), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
                Text(stringResource(R.string.api_log_elapsed, log.elapsedMs?.toString() ?: unknown), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            }
            if (!log.errorMessage.isNullOrBlank() && log.status != "ERROR") {
                Text(stringResource(R.string.api_log_message, log.errorMessage), fontSize = 10.sp, color = sumiInk.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun ApiLogPromptSection(log: ApiDebugLog, context: Context, sumiInk: Color) {
    val clipboardManager = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.api_details_prompts), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sumiInk)
            TextButton(
                onClick = {
                    val logPromptText = "${context.getString(R.string.api_debug_system_prompt)}:\n${log.systemPromptPreview}\n\n${context.getString(R.string.api_debug_user_prompt)}:\n${log.userPrompt}"
                    clipboardManager.setText(AnnotatedString(logPromptText))
                    showCopyToast(context)
                },
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.api_log_copy_prompt), fontSize = 10.sp)
            }
        }
        Divider(color = sumiInk.copy(alpha = 0.1f))

        Text(stringResource(R.string.api_debug_system_prompt), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sumiInk.copy(alpha = 0.7f))
        ApiLogCodeBlock(text = log.systemPromptPreview, color = sumiInk, background = sumiInk.copy(alpha = 0.05f))

        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.api_debug_user_prompt), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sumiInk.copy(alpha = 0.7f))
        ApiLogCodeBlock(text = log.userPrompt, color = sumiInk, background = sumiInk.copy(alpha = 0.05f))
    }
}

@Composable
private fun ApiLogResponseSection(
    formattedResponse: String,
    context: Context,
    sumiInk: Color
) {
    val clipboardManager = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.api_details_response), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sumiInk)
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(formattedResponse))
                    showCopyToast(context)
                },
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.api_log_copy_response), fontSize = 10.sp)
            }
        }
        Divider(color = sumiInk.copy(alpha = 0.1f))
        ApiLogCodeBlock(text = formattedResponse, color = sumiInk, background = sumiInk.copy(alpha = 0.05f))
    }
}

@Composable
private fun ApiLogErrorSection(log: ApiDebugLog, context: Context) {
    val clipboardManager = LocalClipboardManager.current
    val errorColor = Color(0xFFC62828)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.api_details_error_info), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = errorColor)
            TextButton(
                onClick = {
                    val errText = "${context.getString(R.string.api_debug_error)}:\n${log.errorMessage}\n\n${context.getString(R.string.api_debug_stack_trace)}:\n${log.stackTrace ?: ""}"
                    clipboardManager.setText(AnnotatedString(errText))
                    showCopyToast(context)
                },
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.api_log_copy_error), fontSize = 10.sp, color = errorColor)
            }
        }
        Divider(color = errorColor.copy(alpha = 0.2f))

        Text(stringResource(R.string.api_debug_error), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = errorColor)
        ApiLogCodeBlock(
            text = log.errorMessage ?: stringResource(R.string.unknown_error),
            color = errorColor,
            background = errorColor.copy(alpha = 0.05f)
        )

        if (!log.stackTrace.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.api_debug_stack_trace), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = errorColor)
            ApiLogCodeBlock(
                text = log.stackTrace,
                color = errorColor.copy(alpha = 0.8f),
                background = errorColor.copy(alpha = 0.05f)
            )
        }
    }
}

@Composable
private fun ApiLogCodeBlock(text: String, color: Color, background: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Text(text, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = color.copy(alpha = 0.8f))
    }
}

private fun showCopyToast(context: Context) {
    android.widget.Toast
        .makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT)
        .show()
}

@Composable
private fun apiStatusLabel(status: String): String {
    return when (status) {
        "ALL" -> stringResource(R.string.api_debug_filter_all)
        "START" -> stringResource(R.string.api_debug_filter_start)
        "SUCCESS" -> stringResource(R.string.api_debug_filter_success)
        "RETRY" -> stringResource(R.string.api_debug_filter_retry)
        "BACKUP" -> stringResource(R.string.api_debug_filter_backup)
        "TIMEOUT" -> stringResource(R.string.api_debug_filter_timeout)
        "ERROR" -> stringResource(R.string.api_debug_filter_error)
        else -> status
    }
}

@Composable
private fun apiStatusColor(status: String): Color {
    val fallback = MaterialTheme.colorScheme.onBackground
    return when (status) {
        "SUCCESS" -> Color(0xFF2E7D32)
        "ERROR", "TIMEOUT" -> Color(0xFFC62828)
        "RETRY", "BACKUP" -> Color(0xFFE65100)
        "START" -> fallback.copy(alpha = 0.65f)
        else -> fallback.copy(alpha = 0.55f)
    }
}
