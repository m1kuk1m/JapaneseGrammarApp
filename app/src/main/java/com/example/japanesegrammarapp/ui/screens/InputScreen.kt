package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.ui.AppViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(navController: NavController, viewModel: AppViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("api_keys", Context.MODE_PRIVATE) }
    val providers = listOf("Gemini", "Vertex AI", "DeepSeek", "Qwen", "OpenAI Compatible")
    
    var selectedProvider by remember {
        mutableStateOf(
            providers.firstOrNull { provider ->
                val key = prefs.getString("${provider}_key", "") ?: ""
                key.isNotBlank()
            } ?: "Gemini"
        )
    }
    var providerExpanded by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    
    val availableModels by viewModel.availableModels.collectAsState()
    var selectedModel by remember(selectedProvider) { mutableStateOf(prefs.getString("${selectedProvider}_selected_model", "") ?: "") }
    var modelExpanded by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val useOcr by viewModel.useOcr.collectAsState()

    val currentOriginalText by viewModel.currentOriginalText.collectAsState()
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(currentOriginalText) {
        textInput = currentOriginalText
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Traditional Japanese Colors
    val SumiInk = Color(0xFF2B2A28)
    val WashiBg = Color(0xFFFCF8F2)
    val AizomeIndigo = Color(0xFFBCCCD4)

    // UI Event Collection
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.NavigateToResult -> {
                    navController.navigate("result")
                }
            }
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var resumeTrigger by remember { mutableStateOf(0) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(selectedProvider, resumeTrigger) {
        val key = prefs.getString("${selectedProvider}_key", "") ?: ""
        val url = prefs.getString("${selectedProvider}_url", "") ?: ""
        if (key.isNotBlank()) {
            viewModel.fetchModels(selectedProvider, url, key)
        }
    }

    LaunchedEffect(availableModels) {
        if (selectedModel.isBlank() && availableModels.isNotEmpty()) {
            selectedModel = availableModels.first()
            prefs.edit().putString("${selectedProvider}_selected_model", selectedModel).apply()
        }
    }

    // Camera & Crop logic
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
            // Fallback to original source image if crop cancelled/failed
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
            // Fallback directly
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
                val success = withContext(Dispatchers.IO) {
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

    // Permission dynamic launcher for Camera
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
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("カメラを使用するには権限が必要です。")
            }
        }
    }

    Scaffold(
        containerColor = WashiBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("文法分析", fontWeight = FontWeight.Bold, color = SumiInk) },
                actions = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.List, contentDescription = "履歴", tint = SumiInk)
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Provider Dropdown
            ExposedDropdownMenuBox(
                expanded = providerExpanded, 
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProvider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("プロバイダー", color = SumiInk.copy(alpha = 0.7f)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SumiInk,
                        unfocusedBorderColor = SumiInk.copy(alpha = 0.3f),
                        focusedLabelColor = SumiInk,
                        unfocusedLabelColor = SumiInk.copy(alpha = 0.7f),
                        focusedTextColor = SumiInk,
                        unfocusedTextColor = SumiInk
                    )
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded, 
                    onDismissRequest = { providerExpanded = false }
                ) {
                    providers.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = SumiInk) },
                            onClick = {
                                selectedProvider = option
                                selectedModel = ""
                                providerExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Model Dropdown & Fetch Button
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded, 
                    onExpandedChange = { modelExpanded = !modelExpanded }, 
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { 
                            selectedModel = it
                            prefs.edit().putString("${selectedProvider}_selected_model", it).apply()
                        },
                        readOnly = false,
                        placeholder = { Text("モデルを選択または入力...", color = SumiInk.copy(alpha = 0.4f)) },
                        label = { Text("モデル", color = SumiInk.copy(alpha = 0.7f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SumiInk,
                            unfocusedBorderColor = SumiInk.copy(alpha = 0.3f),
                            focusedLabelColor = SumiInk,
                            unfocusedLabelColor = SumiInk.copy(alpha = 0.7f),
                            focusedTextColor = SumiInk,
                            unfocusedTextColor = SumiInk
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded, 
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        availableModels.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = SumiInk) },
                                onClick = {
                                    selectedModel = option
                                    prefs.edit().putString("${selectedProvider}_selected_model", option).apply()
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
                
                FilledTonalIconButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
                        val key = prefs.getString("${selectedProvider}_key", "") ?: ""
                        val url = prefs.getString("${selectedProvider}_url", "") ?: ""
                        viewModel.fetchModels(selectedProvider, url, key)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = SumiInk.copy(alpha = 0.08f),
                        contentColor = SumiInk
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isFetchingModels) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SumiInk)
                    else Icon(Icons.Default.Refresh, contentDescription = "モデルリストの更新")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OCR Settings Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ローカルOCRでテキスト抽出", fontWeight = FontWeight.Bold, color = SumiInk)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = textInput,
                onValueChange = { 
                    textInput = it 
                    viewModel.setCurrentOriginalText(it)
                },
                label = { Text("分析する日本語を入力してください", color = SumiInk.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
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

            // Image Preview (if not OCR and image attached)
            if (!useOcr && selectedImageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "添付画像",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("分析対象の画像", fontWeight = FontWeight.Bold, color = SumiInk, fontSize = 14.sp)
                            Text("マルチモーダルモデルに直接送信されます。", fontSize = 11.sp, color = SumiInk.copy(alpha = 0.5f))
                        }
                        IconButton(onClick = { selectedImageUri = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color(0xFFD32F2F))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Camera & Gallery actions row
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SumiInk),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
                ) {
                    Text("カメラで撮影", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") }, 
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SumiInk),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
                ) {
                    Text("画像を選択", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Analyze start button
            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
                    val key = prefs.getString("${selectedProvider}_key", "") ?: ""
                    val url = prefs.getString("${selectedProvider}_url", "") ?: ""
                    viewModel.analyzeText(textInput, selectedImageUri, selectedProvider, selectedModel.ifBlank { "default" }, url, key)
                    selectedImageUri = null // clear preview after submission
                },
                enabled = (textInput.isNotBlank() || selectedImageUri != null) && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SumiInk,
                    contentColor = WashiBg,
                    disabledContainerColor = SumiInk.copy(alpha = 0.12f),
                    disabledContentColor = SumiInk.copy(alpha = 0.38f)
                )
            ) {
                Text("分析開始", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
