package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.data.AnalysisRecord
import com.example.japanesegrammarapp.network.DetailedAnalysisResult
import com.example.japanesegrammarapp.ui.AppViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(navController: NavController, viewModel: AppViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    val selectedRecord by viewModel.selectedRecord.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    
    var recordToDelete by remember { mutableStateOf<AnalysisRecord?>(null) }

    // UI Event Collection
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
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
                        text = "文法分析履歴",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk
                    )
                    IconButton(
                        onClick = {
                            viewModel.clearSelectedRecord()
                            coroutineScope.launch { drawerState.close() }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新規分析", tint = SumiInk)
                    }
                }
                
                Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // New Analysis Button
                OutlinedButton(
                    onClick = {
                        viewModel.clearSelectedRecord()
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SumiInk),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "新規分析")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新規分析を作成", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Drawer History List
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "分析履歴はありません。",
                            color = SumiInk.copy(alpha = 0.4f),
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
                        items(history, key = { it.id }) { record ->
                            val isSelected = selectedRecord?.id == record.id
                            HistorySidebarItem(
                                record = record,
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.selectRecord(record)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                onLongClick = {
                                    recordToDelete = record
                                }
                            )
                        }
                    }
                }
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
                            text = if (selectedRecord == null) "日本語文法分析" else "分析結果",
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
                        if (selectedRecord != null) {
                            IconButton(onClick = { viewModel.clearSelectedRecord() }) {
                                Icon(Icons.Default.Add, contentDescription = "新規分析", tint = SumiInk)
                            }
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
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
                // Workspace Layout Logic (Google Search B style)
                val hasResult = selectedRecord != null

                if (!hasResult) {
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
                            // Elegant App Logo / Header
                            Text(
                                text = "文法分析",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "AIで日本語の構造を詳細に解釈します",
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
                                    WorkspaceInputForm(viewModel = viewModel, navController = navController)
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
                        // Collapsed top-aligned input box (saves vertical space)
                        var isInputExpanded by remember { mutableStateOf(false) }

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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "分析中のテキスト",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SumiInk.copy(alpha = 0.6f)
                                    )
                                    TextButton(
                                        onClick = { isInputExpanded = !isInputExpanded },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                                    ) {
                                        Text(
                                            text = if (isInputExpanded) "非表示" else "編集・設定",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isInputExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isInputExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    WorkspaceInputForm(viewModel = viewModel, navController = navController)
                                } else {
                                    // Compact text summary
                                    val currentText by viewModel.currentOriginalText.collectAsState()
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
                        }

                        // Detailed result rendering
                        val record = selectedRecord!!
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (record.status) {
                                "PENDING" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = SumiInk)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "文法構造を解析中...",
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "大模型が返答するまで数秒かかる場合があります。",
                                            color = SumiInk.copy(alpha = 0.4f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
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
                                                text = "分析中にエラーが発生しました",
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
                                    // Display actual analysis results (imported cleanly from original design)
                                    WorkspaceResultContent(viewModel = viewModel)
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
            text = { Text("「${record.originalText.take(15)}...」的分析履歴を削除しますか？", color = SumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedRecord?.id == record.id) {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistorySidebarItem(
    record: AnalysisRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(record.timestamp))

    val isPending = record.status == "PENDING"
    val isFailed = record.status == "FAILED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AizomeIndigo.copy(alpha = 0.25f) else Color.White
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = when {
                isFailed -> Color(0xFFD32F2F).copy(alpha = 0.3f)
                isSelected -> SumiInk
                else -> SumiInk.copy(alpha = 0.08f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (!isPending) onClick() },
                    onLongClick = { onLongClick() }
                )
                .padding(12.dp)
        ) {
            Text(
                text = record.originalText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = SumiInk
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (record.status) {
                                    "PENDING" -> Color(0xFFECCEB1)
                                    "FAILED" -> Color(0xFFD32F2F)
                                    else -> Color(0xFFC5E2C6)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (record.status) {
                            "PENDING" -> "分析中"
                            "FAILED" -> "エラー"
                            else -> record.modelUsed.substringAfter(": ").take(12)
                        },
                        fontSize = 11.sp,
                        color = SumiInk.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = SumiInk.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Traditional Japanese Zen Color Palette
private val SumiInk = Color(0xFF2B2A28)          // 墨色 (Deep charcoal black)
private val WashiBg = Color(0xFFFCF8F2)          // 和纸色 (Soft warm white cream)
private val SakuraPink = Color(0xFFFEDFE1)       // 樱花色 (Soft warm blush pink)
private val MatchaGreen = Color(0xFFC5E2C6)      // 抹茶色 (Soft sage green)
private val AizomeIndigo = Color(0xFFBCCCD4)     // 蓝染色 (Soft slate blue)
private val KuriAmber = Color(0xFFECCEB1)        // 栗色 (Soft amber/chestnut)
private val HaiMist = Color(0xFFE5E4E2)          // 雾灰色 (Soft grey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceInputForm(viewModel: AppViewModel, navController: NavController) {
    val context = LocalContext.current
    val prefs = viewModel.securePrefs
    
    val activeProvider by viewModel.activeProvider.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val providerModels by viewModel.providerModels.collectAsState()
    val modelsList = providerModels[activeProvider] ?: emptyList()
    var modelExpanded by remember { mutableStateOf(false) }

    var textInput by remember { mutableStateOf("") }
    val selectedRecord by viewModel.selectedRecord.collectAsState()
    val isAnalyzing = selectedRecord?.status == "PENDING"
    val useOcr by viewModel.useOcr.collectAsState()

    val currentOriginalText by viewModel.currentOriginalText.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentOriginalText) {
        textInput = currentOriginalText
    }

    // Launchers
    val cameraFile = remember { java.io.File(context.cacheDir, "camera_capture.jpg") }
    val cameraFileUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.japanesegrammarapp.fileprovider",
            cameraFile
        )
    }

    val cropFile = remember { java.io.File(context.cacheDir, "cropped_image.jpg") }
    val cropFileUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.japanesegrammarapp.fileprovider",
            cropFile
        )
    }

    val galleryTempFile = remember { java.io.File(context.cacheDir, "gallery_temp.jpg") }
    val galleryTempFileUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.japanesegrammarapp.fileprovider",
            galleryTempFile
        )
    }

    var lastCropSourceUri by remember { mutableStateOf<Uri?>(null) }

    fun handleImageCaptured(uri: Uri) {
        if (useOcr) {
            coroutineScope.launch {
                val extracted = viewModel.extractTextFromImage(uri)
                textInput = extracted
                viewModel.setCurrentOriginalText(extracted)
            }
        } else {
            selectedImageUri = uri
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            handleImageCaptured(Uri.fromFile(cropFile))
        } else {
            lastCropSourceUri?.let { fallbackUri ->
                handleImageCaptured(fallbackUri)
            }
        }
    }

    fun launchCrop(sourceUri: Uri) {
        lastCropSourceUri = sourceUri
        try {
            val intent = android.content.Intent("com.android.camera.action.CROP").apply {
                setDataAndType(sourceUri, "image/*")
                putExtra("crop", "true")
                putExtra("aspectX", 1)
                putExtra("aspectY", 1)
                putExtra("scale", true)
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cropFileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                try {
                    context.grantUriPermission(packageName, sourceUri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (se: SecurityException) {
                    se.printStackTrace()
                }
                try {
                    context.grantUriPermission(packageName, cropFileUri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (se: SecurityException) {
                    se.printStackTrace()
                }
            }
            cropLauncher.launch(intent)
        } catch (e: Exception) {
            handleImageCaptured(sourceUri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            launchCrop(cameraFileUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            java.io.FileOutputStream(galleryTempFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
                if (success) {
                    launchCrop(galleryTempFileUri)
                } else {
                    launchCrop(sourceUri)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                if (cameraFile.exists()) cameraFile.delete()
                cameraFile.createNewFile()
                cameraLauncher.launch(cameraFileUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Sleek, low-prominence Model Selection Pill Chip (Zen Style - No Emojis)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "モデル: ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SumiInk.copy(alpha = 0.5f)
            )
            Box {
                Surface(
                    color = SumiInk.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)),
                    modifier = Modifier.clickable { modelExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$activeProvider : $activeModel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "選択",
                            tint = SumiInk.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    modelsList.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = SumiInk, fontSize = 13.sp) },
                            onClick = {
                                viewModel.setActiveModel(option)
                                modelExpanded = false
                            }
                        )
                    }
                    if (modelsList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("モデル未設定 (設定へ)", color = SumiInk.copy(alpha = 0.5f), fontSize = 13.sp) },
                            onClick = {
                                modelExpanded = false
                                navController.navigate("settings")
                            }
                        )
                    }
                }
            }
        }
        
        TextButton(
            onClick = { navController.navigate("settings") },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "設定",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SumiInk.copy(alpha = 0.6f)
            )
        }
    }

    // OCR Settings Switch
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("ローカルOCRでテキスト抽出", fontWeight = FontWeight.Bold, color = SumiInk, fontSize = 14.sp)
            Text(
                "オフにすると、画像を直接マルチモーダル（Vision）モデルに送信して分析します。",
                fontSize = 11.sp,
                color = SumiInk.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = useOcr,
            onCheckedChange = { viewModel.setUseOcr(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = WashiBg,
                checkedTrackColor = SumiInk,
                uncheckedThumbColor = SumiInk.copy(alpha = 0.4f),
                uncheckedTrackColor = SumiInk.copy(alpha = 0.1f)
            )
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    OutlinedTextField(
        value = textInput,
        onValueChange = { 
            textInput = it 
            viewModel.setCurrentOriginalText(it)
        },
        label = { Text("分析する日本語を入力してください", color = SumiInk.copy(alpha = 0.7f)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(8.dp),
        maxLines = 10,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SumiInk,
            unfocusedBorderColor = SumiInk.copy(alpha = 0.3f),
            focusedLabelColor = SumiInk,
            unfocusedLabelColor = SumiInk.copy(alpha = 0.7f),
            focusedTextColor = SumiInk,
            unfocusedTextColor = SumiInk
        )
    )

    // Image Preview
    if (!useOcr && selectedImageUri != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "添付画像",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("分析対象の画像", fontWeight = FontWeight.Bold, color = SumiInk, fontSize = 12.sp)
                    Text("マルチモーダルに直接送信されます。", fontSize = 10.sp, color = SumiInk.copy(alpha = 0.5f))
                }
                IconButton(onClick = { selectedImageUri = null }) {
                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color(0xFFD32F2F))
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Camera & Gallery actions
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                )
                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    try {
                        if (cameraFile.exists()) cameraFile.delete()
                        cameraFile.createNewFile()
                        cameraLauncher.launch(cameraFileUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            }, 
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SumiInk),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
        ) {
            Text("カメラで撮影", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") }, 
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SumiInk),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
        ) {
            Text("画像を選択", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Main start button
    Button(
        onClick = {
            val key = prefs.getString("${activeProvider}_key", "") ?: ""
            val url = prefs.getString("${activeProvider}_url", "") ?: ""
            viewModel.analyzeText(textInput, selectedImageUri, activeProvider, activeModel.ifBlank { "default" }, url, key)
            selectedImageUri = null
        },
        enabled = (textInput.isNotBlank() || selectedImageUri != null) && !isAnalyzing,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SumiInk,
            contentColor = WashiBg,
            disabledContainerColor = SumiInk.copy(alpha = 0.12f),
            disabledContentColor = SumiInk.copy(alpha = 0.38f)
        )
    ) {
        Text("分析開始", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkspaceResultContent(viewModel: AppViewModel) {
    val detailedResult by viewModel.detailedResult.collectAsState()
    val rawResult by viewModel.analysisResult.collectAsState()

    if (detailedResult != null) {
        val data = detailedResult!!
        var selectedSegmentIndex by remember(data) { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // 1. Target Sentence Header with clickable chips
            Text(
                text = "対象の例文",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SumiInk.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.segments?.forEachIndexed { index, segment ->
                        val isSelected = index == selectedSegmentIndex
                        val borderWidth by animateDpAsState(
                            targetValue = if (isSelected) 2.dp else 1.dp,
                            label = "borderWidth"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) SumiInk else SumiInk.copy(alpha = 0.15f),
                            label = "borderColor"
                        )
                        Surface(
                            color = getChipColorForPos(segment.partOfSpeech ?: ""),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(width = borderWidth, color = borderColor),
                            modifier = Modifier
                                .clickable { selectedSegmentIndex = index }
                                .padding(horizontal = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = segment.reading ?: "",
                                    fontSize = 9.sp,
                                    color = SumiInk.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = segment.text ?: "",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SumiInk
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 2. Segment Details Explanation Box
            val currentSegment = data.segments?.getOrNull(selectedSegmentIndex)
            if (currentSegment != null) {
                Text(
                    text = "単語の分解と分析 (タップして選択中)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentSegment.text ?: "",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk
                            )
                            Text(
                                text = "（${currentSegment.reading ?: ""}）",
                                fontSize = 12.sp,
                                color = SumiInk.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = SumiInk.copy(alpha = 0.1f)
                        )
                        
                        DetailRow(label = "品詞", value = currentSegment.partOfSpeech ?: "")
                        
                        if (!currentSegment.dictionaryForm.isNullOrBlank()) {
                            DetailRow(label = "辞書形", value = currentSegment.dictionaryForm)
                        }
                        
                        if (!currentSegment.inflection.isNullOrBlank()) {
                            DetailRow(label = "構成/活用", value = currentSegment.inflection)
                        }
                        
                        DetailRow(label = "役割", value = currentSegment.role ?: "")
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = SumiInk.copy(alpha = 0.1f)
                        )
                        
                        Surface(
                            color = KuriAmber.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, KuriAmber.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "中国語の訳・意味",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentSegment.meaning ?: "",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 3. Overall Sentence Translation
            Text(
                text = "全体の翻訳と文の種類",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SumiInk.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SakuraPink,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "翻訳",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = data.translation ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SumiInk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 4. Sentence Clauses
            if (!data.clauses.isNullOrEmpty()) {
                Text(
                    text = "文節の解釈と構造",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        data.clauses?.forEachIndexed { idx, clause ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${clause.index}.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk,
                                    modifier = Modifier.width(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = AizomeIndigo.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Text(
                                                text = clause.role ?: "",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SumiInk,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = clause.text ?: "",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = clause.explanation ?: "",
                                        fontSize = 12.sp,
                                        color = SumiInk.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (idx < data.clauses.size - 1) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = SumiInk.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 5. Core Grammar Points
            if (!data.grammarPoints.isNullOrEmpty()) {
                Text(
                    text = "文法ポイントの分析",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        data.grammarPoints?.forEachIndexed { idx, gp ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MatchaGreen,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "文法",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = gp.pattern ?: "",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SumiInk
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = gp.explanation ?: "",
                                    fontSize = 13.sp,
                                    color = SumiInk.copy(alpha = 0.8f),
                                    lineHeight = 18.sp
                                )
                            }
                            if (idx < (data.grammarPoints?.size ?: 0) - 1) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SumiInk.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        // Robust Fallback: Show original plain text result if detailedResult is null (backward compatibility)
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "分析結果 (テキスト表示)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Divider(color = SumiInk.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = rawResult ?: "分析結果はありません。",
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SumiInk,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Morandi color coding helper
private fun getChipColorForPos(pos: String): Color {
    return when {
        pos.contains("名詞") -> Color(0xFFD3E0EA) // 蓝染蓝 (Aizome)
        pos.contains("動詞") -> Color(0xFFD4ECD5) // 抹茶绿 (Matcha)
        pos.contains("形容") || pos.contains("形状") -> Color(0xFFF6E2CD) // 栗色 (Kuri)
        pos.contains("助動詞") -> Color(0xFFE8D3EA) // 藤紫 (Fuji)
        pos.contains("助詞") -> Color(0xFFFDD4D8) // 樱花粉 (Sakura)
        else -> Color(0xFFEFEFEF) // 雾灰 (Hai)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = SumiInk.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SumiInk
        )
    }
}
