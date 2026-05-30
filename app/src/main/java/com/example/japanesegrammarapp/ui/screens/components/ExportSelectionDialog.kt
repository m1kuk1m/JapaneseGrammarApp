package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.data.AnalysisRecord
import androidx.compose.ui.res.stringResource
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportSelectionDialog(
    historyList: List<AnalysisRecord>,
    onDismiss: () -> Unit,
    onExportSelected: (List<AnalysisRecord>) -> Unit
) {
    // Keep track of selected item IDs in a map
    // We pre-select all records by default for a better user experience
    val selectedMap = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            historyList.forEach { put(it.id, true) }
        }
    }

    val selectedCount = selectedMap.values.count { it }
    val isAllSelected = selectedCount == historyList.size && historyList.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WashiBg),
            border = BorderStroke(1.5.dp, SumiInk.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.export_history_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = SumiInk.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select All Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val targetState = !isAllSelected
                            historyList.forEach { selectedMap[it.id] = targetState }
                        }
                        .background(SumiInk.copy(alpha = 0.04f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = SumiInk,
                            uncheckedColor = SumiInk.copy(alpha = 0.5f),
                            checkmarkColor = WashiBg
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAllSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SumiInk
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of History Items
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList, key = { it.id }) { record ->
                        val isSelected = selectedMap[record.id] == true
                        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        val dateStr = sdf.format(Date(record.timestamp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SumiInk.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) SumiInk.copy(alpha = 0.15f) else SumiInk.copy(alpha = 0.06f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMap[record.id] = !isSelected }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SumiInk,
                                        uncheckedColor = SumiInk.copy(alpha = 0.5f),
                                        checkmarkColor = WashiBg
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.originalText.ifBlank { stringResource(R.string.image_analysis) },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = SumiInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        when (record.status) {
                                                            "PENDING" -> KuriAmber
                                                            "FAILED" -> Color(0xFFD32F2F)
                                                            else -> MatchaGreen
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = when (record.status) {
                                                    "PENDING" -> stringResource(R.string.history_status_pending)
                                                    "FAILED" -> stringResource(R.string.history_status_error)
                                                    else -> record.modelUsed.substringAfter(": ").take(15)
                                                },
                                                fontSize = 10.sp,
                                                color = SumiInk.copy(alpha = 0.5f)
                                            )
                                        }
                                        Text(
                                            text = dateStr,
                                            fontSize = 10.sp,
                                            color = SumiInk.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                    ) {
                        Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val selectedRecords = historyList.filter { selectedMap[it.id] == true }
                            onExportSelected(selectedRecords)
                        },
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SumiInk,
                            contentColor = WashiBg,
                            disabledContainerColor = SumiInk.copy(alpha = 0.12f),
                            disabledContentColor = SumiInk.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(
                            text = if (selectedCount > 0) stringResource(R.string.export_with_count, selectedCount) else stringResource(R.string.export),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
