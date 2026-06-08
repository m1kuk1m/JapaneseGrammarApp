package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.utils.AppLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogsDialog(
    logs: List<String>,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("ALL") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val filteredLogs = remember(logs, searchQuery, selectedLevel) {
        logs.filter { log ->
            val matchesQuery = searchQuery.isBlank() || log.contains(searchQuery, ignoreCase = true)
            val matchesLevel = when (selectedLevel) {
                "DEBUG" -> log.contains(" D/")
                "ERROR" -> log.contains(" E/")
                else -> true
            }
            matchesQuery && matchesLevel
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.app_logs_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_logs),
                        tint = sumiInk
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
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
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val levels = listOf("ALL", "DEBUG", "ERROR")
                    val levelLabels = mapOf(
                        "ALL" to stringResource(R.string.log_level_all),
                        "DEBUG" to stringResource(R.string.log_level_debug),
                        "ERROR" to stringResource(R.string.log_level_error)
                    )
                    levels.forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(levelLabels[level] ?: level, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = onPrimaryColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { AppLogger.clear() }) {
                            Text(stringResource(R.string.clear_logs), color = Color.Red, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(filteredLogs.joinToString("\n")))
                            android.widget.Toast
                                .makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT)
                                .show()
                        }) {
                            Text(stringResource(R.string.copy_logs), fontSize = 12.sp)
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.scroll_to_top))
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (filteredLogs.isNotEmpty()) {
                                        listState.animateScrollToItem(filteredLogs.size - 1)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.scroll_to_bottom))
                        }
                    }
                }

                val logSize = remember(logs) { AppLogger.getLogFileSize(context) }
                Text(
                    text = stringResource(R.string.log_size_label, logSize),
                    fontSize = 10.sp,
                    color = sumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = sumiInk.copy(alpha = 0.1f))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().weight(1f).padding(vertical = 4.dp)
                ) {
                    items(filteredLogs) { log ->
                        val color = when {
                            log.contains(" E/") -> Color(0xFFC62828)
                            log.contains(" D/") -> sumiInk.copy(alpha = 0.6f)
                            else -> sumiInk
                        }
                        Text(
                            text = log,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp,
                            color = color,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Divider(modifier = Modifier.padding(vertical = 2.dp), color = sumiInk.copy(alpha = 0.05f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
