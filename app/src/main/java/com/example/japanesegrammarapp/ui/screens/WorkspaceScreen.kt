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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.WorkspaceViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import com.example.japanesegrammarapp.ui.screens.components.ExportSelectionDialog
import com.example.japanesegrammarapp.ui.screens.components.HistorySidebar
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceInputForm
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceResultContent
import com.example.japanesegrammarapp.ui.screens.components.ZenLoadingView
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(navController: NavController, viewModel: WorkspaceViewModel) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val PrimaryColor = MaterialTheme.colorScheme.primary
    val OnPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    
    var recordToDelete by remember { mutableStateOf<AnalysisDomainRecord?>(null) }
    val currentHistory by rememberUpdatedState(history)
    var showExportDialog by remember { mutableStateOf(false) }
    
    val isPlayingTts by viewModel.isPlayingTts.collectAsState(initial = false)

    // Hoisted States for input form
    var textInputState by androidx.compose.runtime.saveable.rememberSaveable(uiState.currentOriginalText) {
        mutableStateOf(uiState.currentOriginalText)
    }
    var selectedImageUriState by remember { mutableStateOf<Uri?>(null) }

    // Intercept back button to close drawer or return to input page
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    androidx.activity.compose.BackHandler(enabled = !drawerState.isOpen && uiState.selectedRecord != null) {
        viewModel.clearSelectedRecord()
    }

    val navigateToSettings: () -> Unit = remember(navController) {
        {
            if (navController.currentDestination?.route == "workspace") {
                navController.navigate("settings")
            }
        }
    }

    // UI Event Collection & Localization Handling
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowLocalizedError -> {
                    val formattedMessage = if (event.args.isNotEmpty()) {
                        val resolvedArgs = event.args.map { arg ->
                            if (arg is Int) context.getString(arg) else arg
                        }
                        context.getString(event.resId, *resolvedArgs.toTypedArray())
                    } else {
                        context.getString(event.resId)
                    }
                    snackbarHostState.showSnackbar(formattedMessage)
                }
                is UiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = context.getString(R.string.show_action),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val completedRecord = currentHistory.find { it.id == event.recordId }
                        if (completedRecord != null) {
                            viewModel.selectRecord(completedRecord)
                        }
                    }
                }
                is UiEvent.ExportRecordEvent -> {
                    val content = com.example.japanesegrammarapp.utils.RecordExporter.buildRecordExportText(context, event.record)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                        putExtra(Intent.EXTRA_SUBJECT, event.filename)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title)))
                }
                is UiEvent.ExportAllHistoryEvent -> {
                    val content = com.example.japanesegrammarapp.utils.RecordExporter.buildAllHistoryExportText(context, event.records)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                        putExtra(Intent.EXTRA_SUBJECT, event.filename)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title)))
                }
                else -> {}
            }
        }
    }

    // Camera Result Observer & OCR Executor
    val navBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.getStateFlow<String?>("captured_image_uri", null)
            ?.collect { uriString ->
                if (!uriString.isNullOrBlank()) {
                    val uri = Uri.parse(uriString)
                    if (uiState.useOcr) {
                        coroutineScope.launch {
                            val extracted = viewModel.extractTextFromImage(uri)
                            textInputState = extracted
                            viewModel.setCurrentOriginalText(extracted)
                            if (extracted.isNotBlank()) {
                                viewModel.startAnalysis(extracted, uri)
                            }
                        }
                    } else {
                        selectedImageUriState = uri
                    }
                    navBackStackEntry.savedStateHandle["captured_image_uri"] = null
                }
            }
    }

    // Modal Drawer wrapper
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg,
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
        Scaffold(
            containerColor = if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg,
            snackbarHost = { 
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SumiInk.copy(alpha = 0.85f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 60.dp, start = 16.dp, end = 16.dp)
                    )
                } 
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.selectedRecord == null) stringResource(R.string.app_name) else stringResource(R.string.analysis_results),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.history_menu_desc), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        if (uiState.selectedRecord != null) {
                            IconButton(onClick = {
                                uiState.selectedRecord?.let { viewModel.exportRecord(it) }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { viewModel.clearSelectedRecord() }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_analysis), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(onClick = navigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg,
                        titleContentColor = PrimaryColor
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
                                    text = stringResource(R.string.app_title),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = stringResource(R.string.app_subtitle),
                                    fontSize = 13.sp,
                                    color = SumiInk.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 28.dp)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        WorkspaceInputForm(
                                            uiState = uiState,
                                            textInput = textInputState,
                                            onTextInputChanged = { text ->
                                                textInputState = text
                                                viewModel.setCurrentOriginalText(text)
                                            },
                                            selectedImageUri = selectedImageUriState,
                                            onSelectedImageUriChanged = { uri -> selectedImageUriState = uri },
                                            onModelSelected = { model -> viewModel.setActiveModel(model) },
                                            onStartAnalysis = { text, uri ->
                                                viewModel.startAnalysis(text, uri)
                                            },
                                            onCancelAnalysis = {
                                                uiState.selectedRecord?.id?.let { viewModel.cancelAnalysis(it) }
                                            },
                                            onNavigateToCamera = { navController.navigate("camera") },
                                            onPickImage = { sourceUri ->
                                                coroutineScope.launch {
                                                    val localUri = withContext(Dispatchers.IO) {
                                                        com.example.japanesegrammarapp.utils.BitmapHelper.copyUriToCache(context, sourceUri)
                                                    }
                                                    val finalUri = localUri ?: sourceUri
                                                    navController.navigate("camera?imageUri=${Uri.encode(finalUri.toString())}")
                                                }
                                            },
                                            onNavigateToSettings = navigateToSettings
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            text = stringResource(R.string.target_sentence),
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
                                                contentDescription = stringResource(R.string.edit_and_reanalyze_desc),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.edit_and_reanalyze),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = currentText.ifBlank { stringResource(R.string.image_analysis) },
                                        fontSize = 14.sp,
                                        color = SumiInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            val record = uiState.selectedRecord
                            if (record != null) {
                                val resultState = when {
                                    record.status == AnalysisStatus.FAILED -> "FAILED"
                                    else -> "CONTENT"
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
                                            "CONTENT" -> {
                                                WorkspaceResultContent(
                                                    uiState = uiState,
                                                    isPlayingTts = isPlayingTts,
                                                    onPlayTts = { viewModel.playTtsForCurrentRecord() },
                                                    onStopTts = { viewModel.stopTts() },
                                                    onCancel = {
                                                        if (record.status == AnalysisStatus.PENDING) {
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
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                                            text = stringResource(R.string.error_occurred),
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD32F2F),
                                                            fontSize = 14.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = record.errorMessage ?: stringResource(R.string.error_occurred),
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
                                                                Text(stringResource(R.string.delete), fontSize = 13.sp)
                                                            }
                                                            Button(
                                                                onClick = { viewModel.retryAnalysis(record.id) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimaryColor)
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry), modifier = Modifier.size(16.dp))
                                                                 Spacer(modifier = Modifier.width(4.dp))
                                                                Text(stringResource(R.string.retry), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                }
            }
        }
    }

    // Deletion Confirmation Dialog
    if (recordToDelete != null) {
        val record = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text(stringResource(R.string.delete_history_title), fontWeight = FontWeight.Bold, color = SumiInk) },
            text = { Text(stringResource(R.string.delete_history_confirm, record.originalText.take(15)), color = SumiInk) },
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
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = SumiInk)
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
