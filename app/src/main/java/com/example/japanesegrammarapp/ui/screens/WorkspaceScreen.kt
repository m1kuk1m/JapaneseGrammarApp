package com.example.japanesegrammarapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.ui.AppViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import com.example.japanesegrammarapp.ui.screens.components.ExportSelectionDialog
import com.example.japanesegrammarapp.ui.screens.components.HistorySidebar
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceInputForm
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceResultContent
import com.example.japanesegrammarapp.ui.screens.components.ZenLoadingView
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(navController: NavController, viewModel: AppViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    
    var recordToDelete by remember { mutableStateOf<AnalysisRecord?>(null) }
    val currentHistory by rememberUpdatedState(history)
    var showExportDialog by remember { mutableStateOf(false) }

    // Single source-of-truth navigation helper. NavController.navigate() synchronously
    // updates currentDestination, so calling this twice in a row is a safe no-op:
    // the second call sees route != "workspace" and exits immediately. No boolean
    // flags, LaunchedEffect timers, or lifecycle observers are needed.
    val navigateToSettings: () -> Unit = remember(navController) {
        {
            if (navController.currentDestination?.route == "workspace") {
                navController.navigate("settings")
            }
        }
    }

    // UI Event Collection
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "表示",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val completedRecord = currentHistory.find { it.id == event.recordId }
                        if (completedRecord != null) {
                            viewModel.selectRecord(completedRecord)
                        }
                    }
                }
                is UiEvent.ExportContent -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.content)
                        putExtra(Intent.EXTRA_SUBJECT, event.filename)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "エクスポート先を選択"))
                }
                else -> {}
            }
        }
    }

    // Modal Drawer wrapper
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = WashiBg,
                modifier = Modifier.width(310.dp).fillMaxHeight()
            ) {
                HistorySidebar(
                    historyList = history,
                    selectedRecord = uiState.selectedRecord,
                    onSelectRecord = { record -> viewModel.selectRecord(record) },
                    onClearSelection = { viewModel.clearSelectedRecord() },
                    onDeleteRecord = { record -> recordToDelete = record },
                    onExportAll = {
                        coroutineScope.launch { drawerState.close() }
                        showExportDialog = true
                    },
                    onExportRecord = { record -> viewModel.exportRecord(record) },
                    onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        // Main Screen Content
        Scaffold(
            containerColor = WashiBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.selectedRecord == null) "日本語文法分析" else "分析結果",
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "履歴メニュー", tint = SumiInk)
                        }
                    },
                    actions = {
                        if (uiState.selectedRecord != null) {
                            // Export current record
                            IconButton(onClick = {
                                uiState.selectedRecord?.let { viewModel.exportRecord(it) }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "エクスポート", tint = SumiInk)
                            }
                            IconButton(onClick = { viewModel.clearSelectedRecord() }) {
                                Icon(Icons.Default.Add, contentDescription = "新規分析", tint = SumiInk)
                            }
                        }
                        IconButton(onClick = navigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "設定", tint = SumiInk)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = WashiBg,
                        titleContentColor = SumiInk
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val hasResult = uiState.selectedRecord != null

                AnimatedContent(
                    targetState = hasResult,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "WorkspaceStateTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetHasResult ->
                    if (!targetHasResult) {
                        // Initial State: Input field centered in the middle of screen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "文法分析",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "AIが日本語の文法構造をわかりやすく解説します",
                                    fontSize = 13.sp,
                                    color = SumiInk.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 28.dp)
                                )

                                // Main Input Panel
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        WorkspaceInputForm(
                                            uiState = uiState,
                                            viewModel = viewModel,
                                            navController = navController,
                                            onNavigateToSettings = navigateToSettings
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Result State: Input box moves to the top, scrollable results expand below
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Read-only text card for current analysis
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val currentText = uiState.currentOriginalText
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "分析対象",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SumiInk.copy(alpha = 0.6f)
                                        )
                                        TextButton(
                                            onClick = {
                                                viewModel.startNewAnalysisWithText(currentText)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "編集して新しく分析",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "編集して再分析",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = currentText.ifBlank { "画像文法分析" },
                                        fontSize = 14.sp,
                                        color = SumiInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Detailed result rendering
                            val record = uiState.selectedRecord
                            if (record != null) {
                                val resultState = when {
                                    record.status == "PENDING" || (record.status == "COMPLETED" && uiState.isParsingDetailedResult) -> "LOADING"
                                    record.status == "FAILED" -> "FAILED"
                                    else -> "COMPLETED"
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    AnimatedContent(
                                        targetState = resultState,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(400, easing = EaseInOutCubic))
                                                .togetherWith(fadeOut(animationSpec = tween(400, easing = EaseInOutCubic)))
                                        },
                                        label = "ResultStateTransition",
                                        modifier = Modifier.fillMaxSize()
                                    ) { state ->
                                        when (state) {
                                            "LOADING" -> {
                                                ZenLoadingView(
                                                    progress = if (record.status == "PENDING") uiState.selectedRecordProgress else null,
                                                    onCancel = {
                                                        if (record.status == "PENDING") {
                                                            viewModel.cancelAnalysis(record.id)
                                                        } else {
                                                            viewModel.clearSelectedRecord()
                                                        }
                                                    }
                                                )
                                            }
                                            "FAILED" -> {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F2)),
                                                    border = BorderStroke(1.dp, Color(0xFFF3D8D8)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "Error",
                                                            tint = Color(0xFFD32F2F),
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text(
                                                            text = "エラーが発生しました",
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD32F2F),
                                                            fontSize = 14.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = record.errorMessage ?: "不明なエラーが発生しました。",
                                                            color = SumiInk.copy(alpha = 0.7f),
                                                            fontSize = 13.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                            OutlinedButton(
                                                                onClick = { viewModel.deleteRecord(record); viewModel.clearSelectedRecord() },
                                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                                                border = BorderStroke(1.dp, Color(0xFFF3D8D8))
                                                            ) {
                                                                Text("削除", fontSize = 13.sp)
                                                            }
                                                            Button(
                                                                onClick = { viewModel.retryAnalysis(record.id) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = SumiInk, contentColor = WashiBg)
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = "再試行", modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("再試行", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "COMPLETED" -> {
                                                WorkspaceResultContent(uiState = uiState)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Deletion Confirmation Dialog
    if (recordToDelete != null) {
        val record = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("履歴の削除", fontWeight = FontWeight.Bold, color = SumiInk) },
            text = { Text("「${record.originalText.take(15)}...」の分析履歴を削除しますか？", color = SumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        if (uiState.selectedRecord?.id == record.id) {
                            viewModel.clearSelectedRecord()
                        }
                        viewModel.deleteRecord(record)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
                ) {
                    Text("削除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("キャンセル", color = SumiInk)
                }
            },
            containerColor = Color.White
        )
    }

    // Export Selection Dialog
    if (showExportDialog) {
        ExportSelectionDialog(
            historyList = history,
            onDismiss = { showExportDialog = false },
            onExportSelected = { selectedRecords ->
                viewModel.exportAllHistory(selectedRecords)
                showExportDialog = false
            }
        )
    }
}
