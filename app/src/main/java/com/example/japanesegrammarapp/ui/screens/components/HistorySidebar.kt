package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.theme.ZenColors.AizomeIndigo
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors
import androidx.compose.material.icons.filled.Delete
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import java.util.*
import com.example.japanesegrammarapp.ui.HistoryUiRecord

@Composable
fun HistorySidebar(
    historyList: LazyPagingItems<HistoryUiRecord>,
    selectedRecord: AnalysisDomainRecord?,
    onSelectRecord: (AnalysisDomainRecord) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteRecord: (AnalysisDomainRecord) -> Unit,
    onExportAll: () -> Unit,
    onExportRecord: (AnalysisDomainRecord) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(WashiBg)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SumiInk
            )
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = SumiInk)
            }
        }
        
        Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // New Analysis Button
        Button(
            onClick = {
                onClearSelection()
                onCloseDrawer()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .height(44.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.new_analysis), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (historyList.itemCount > 0) {
            Button(
                onClick = onExportAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZenThemeColors.buttonBg(),
                    contentColor = SumiInk
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.export_all), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.export_all), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drawer History List
        if (historyList.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_history),
                    color = SumiInk.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = historyList.itemCount,
                    key = historyList.itemKey { it.record.id },
                    contentType = historyList.itemContentType { "history_record_item" }
                ) { index ->
                    val uiRecord = historyList[index]
                    if (uiRecord != null) {
                        val record = uiRecord.record
                        val isSelected = selectedRecord?.id == record.id
                        val onSelect = remember(record.id) { {
                            onSelectRecord(record)
                            onCloseDrawer()
                        } }
                        val onDelete = remember(record.id) { { onDeleteRecord(record) } }
                        val onExport = remember(record.id) { { onExportRecord(record) } }
    
                        HistorySidebarItem(
                            uiRecord = uiRecord,
                            isSelected = isSelected,
                            onClick = onSelect,
                            onDeleteClick = onDelete,
                            onExportClick = onExport
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySidebarItem(
    uiRecord: HistoryUiRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val record = uiRecord.record
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val PrimaryColor = MaterialTheme.colorScheme.primary

    val ItemTextColor = SumiInk
    val ItemSubTextColor = SumiInk.copy(alpha = 0.5f)
    val ItemIconColor = SumiInk.copy(alpha = 0.4f)
    val ItemPrimaryIconColor = PrimaryColor.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ZenThemeColors.selectedHistoryBg() else SurfaceColor,
        border = null,
        shadowElevation = if (isSelected) 0.dp else 0.5.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Text(
                text = record.originalText,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = ItemTextColor
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when (record.status) {
                                    AnalysisStatus.PENDING -> KuriAmber
                                    AnalysisStatus.FAILED -> Color(0xFFD32F2F)
                                    else -> MatchaGreen
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (record.status) {
                            AnalysisStatus.PENDING -> stringResource(R.string.history_status_pending)
                            AnalysisStatus.FAILED -> stringResource(R.string.history_status_error)
                            else -> uiRecord.modelText
                        },
                        fontSize = 11.sp,
                        color = ItemSubTextColor
                    )
                    if (uiRecord.formattedTokens != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tokens: ${uiRecord.formattedTokens}",
                            fontSize = 11.sp,
                            color = ItemSubTextColor
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = uiRecord.dateStr,
                        fontSize = 11.sp,
                        color = ItemSubTextColor
                    )
                    IconButton(
                        onClick = onExportClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.export),
                            tint = ItemPrimaryIconColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = ItemIconColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
