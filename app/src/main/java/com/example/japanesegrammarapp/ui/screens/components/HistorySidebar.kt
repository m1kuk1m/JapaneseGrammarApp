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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Upload
import kotlinx.coroutines.launch
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
    onCloseDrawer: () -> Unit,
    onImportHistory: (String) -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: ""
                    if (text.isNotBlank()) {
                        onImportHistory(text)
                    }
                } catch (e: Exception) {
                    // Failures are handled internally or logged
                }
            }
        }
    }

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

        // New Analysis Button (Full-width)
        Button(
            onClick = {
                onClearSelection()
                onCloseDrawer()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .height(44.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.new_analysis), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Secondary Actions Row: Import & Export All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Import History Button
            Button(
                onClick = {
                    filePickerLauncher.launch("text/plain")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZenThemeColors.buttonBg(),
                    contentColor = SumiInk
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.import_history), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.import_history), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (historyList.itemCount > 0) {
                // Export All Button
                Button(
                    onClick = onExportAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZenThemeColors.buttonBg(),
                        contentColor = SumiInk
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = stringResource(R.string.export_all), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.export_all), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ZenThemeColors.selectedHistoryBg() else SurfaceColor,
        border = null,
        shadowElevation = if (isSelected) 0.dp else 0.5.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = record.originalText,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = ItemTextColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Line 1: Status badge & Info (left-aligned)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                
                val statusText = when (record.status) {
                    AnalysisStatus.PENDING -> stringResource(R.string.history_status_pending)
                    AnalysisStatus.FAILED -> stringResource(R.string.history_status_error)
                    else -> uiRecord.modelText
                }
                val tokenText = if (uiRecord.formattedTokens != null) "  Tokens: ${uiRecord.formattedTokens}" else ""
                
                Text(
                    text = "$statusText$tokenText",
                    fontSize = 11.sp,
                    color = ItemSubTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Line 2: Date & Action Buttons (right-aligned)
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = uiRecord.dateStr,
                    fontSize = 11.sp,
                    color = ItemSubTextColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier.size(30.dp)
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
                    modifier = Modifier.size(30.dp)
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
