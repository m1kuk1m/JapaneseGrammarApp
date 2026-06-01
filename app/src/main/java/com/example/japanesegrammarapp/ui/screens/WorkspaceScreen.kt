package com.example.japanesegrammarapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.WorkspaceViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import com.example.japanesegrammarapp.ui.screens.components.ExportSelectionDialog
import com.example.japanesegrammarapp.ui.screens.components.FloatingActionBall
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
import androidx.paging.compose.collectAsLazyPagingItems

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
    val history = viewModel.history.collectAsLazyPagingItems()
    val allHistoryForExport by viewModel.allHistoryForExport.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var recordToDelete by remember { mutableStateOf<AnalysisDomainRecord?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    val isPlayingTts by viewModel.isPlayingTts.collectAsState(initial = false)

    // Hoisted States for input form
    var textInputState by androidx.compose.runtime.saveable.rememberSaveable(uiState.currentOriginalText) {
        mutableStateOf(uiState.currentOriginalText)
    }
    var selectedImageUriState by remember { mutableStateOf<Uri?>(null) }

    // Clear image uri when returning to homepage (no active record)
    LaunchedEffect(uiState.selectedRecord) {
        if (uiState.selectedRecord == null) {
            selectedImageUriState = null
        }
    }

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

    // Refresh settings when returning to workspace
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                    val formattedMessage = try {
                        if (event.args.isNotEmpty()) {
                            val resolvedArgs = event.args.map { arg ->
                                if (arg is Int) {
                                    try {
                                        context.getString(arg)
                                    } catch (e: android.content.res.Resources.NotFoundException) {
                                        arg
                                    }
                                } else arg
                            }
                            context.getString(event.resId, *resolvedArgs.toTypedArray())
                        } else {
                            context.getString(event.resId)
                        }
                    } catch (e: android.content.res.Resources.NotFoundException) {
                        "Notification triggered"
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
                        viewModel.selectRecordById(event.recordId)
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
    val cameraNavBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(cameraNavBackStackEntry) {
        cameraNavBackStackEntry?.savedStateHandle?.getStateFlow<String?>("captured_image_uri", null)
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
                        viewModel.startAnalysis("", uri)
                    }
                    cameraNavBackStackEntry.savedStateHandle["captured_image_uri"] = null
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
                        viewModel.loadAllHistoryForExport()
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
                if (uiState.selectedRecord != null) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.analysis_results),
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
                                    uiState.selectedRecord?.let { viewModel.retryAnalysis(it.id) }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry), tint = MaterialTheme.colorScheme.onSurface)
                                }
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            // Custom Top Bar for Elegant UI
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.history_menu_desc), tint = SumiInk)
                                }
                                IconButton(
                                    onClick = navigateToSettings,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = SumiInk)
                                }
                            }

                            // Hero Typography
                            Text(
                                text = stringResource(R.string.hero_title),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = SumiInk,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.hero_subtitle),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Light,
                                color = SumiInk.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 72.dp)
                            )

                            // Main Floating Panel
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 40.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 3.dp,
                                tonalElevation = 0.dp
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
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
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                             Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 3.dp,
                                tonalElevation = 0.dp
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    uiState.selectedRecord?.let { viewModel.retryAnalysis(it.id) }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = stringResource(R.string.retry),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.retry),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
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
                                                         androidx.compose.foundation.text.selection.SelectionContainer {
                                                             Text(
                                                                 text = record.errorMessage ?: stringResource(R.string.error_occurred),
                                                                 color = SumiInk.copy(alpha = 0.7f),
                                                                 fontSize = 13.sp,
                                                                 textAlign = TextAlign.Center
                                                             )
                                                         }
                                                         Spacer(modifier = Modifier.height(12.dp))
                                                         val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                                         val errorStr = record.errorMessage ?: stringResource(R.string.error_occurred)
                                                         TextButton(
                                                             onClick = {
                                                                 clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(errorStr))
                                                             },
                                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                             colors = ButtonDefaults.textButtonColors(contentColor = SumiInk.copy(alpha = 0.8f))
                                                         ) {
                                                             Icon(
                                                                 imageVector = Icons.Default.ContentCopy,
                                                                 contentDescription = "Copy Error Details",
                                                                 modifier = Modifier.size(14.dp)
                                                             )
                                                             Spacer(modifier = Modifier.width(4.dp))
                                                             Text("复制完整报错日志", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                         }
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
            historyList = allHistoryForExport,
            onDismiss = { showExportDialog = false },
            onExportSelected = { selectedRecords ->
                viewModel.exportAllHistory(selectedRecords)
                showExportDialog = false
            }
        )
    }

    // Input Dialog for Floating Action Ball
    var showInputDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    
    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("文字入力", fontWeight = FontWeight.Bold, color = SumiInk) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text("分析したいテキストを入力してください...") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = SumiInk.copy(alpha = 0.2f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.startNewAnalysisWithText(inputText)
                        }
                        showInputDialog = false
                        inputText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimaryColor)
                ) {
                    Text("分析開始", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text(stringResource(R.string.cancel), color = SumiInk)
                }
            },
            containerColor = Color.White
        )
    }

    // Floating Action Ball
    FloatingActionBall(
        onTextClick = { showInputDialog = true },
        onCameraClick = { navController.navigate("camera") }
    )
}
