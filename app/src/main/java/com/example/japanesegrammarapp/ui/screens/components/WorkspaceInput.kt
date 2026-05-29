package com.example.japanesegrammarapp.ui.screens.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.japanesegrammarapp.ui.WorkspaceUiState
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceInputForm(
    uiState: WorkspaceUiState,
    viewModel: AppViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    
    val activeProvider = uiState.activeProvider
    val activeModel = uiState.activeModel
    val providerModels = uiState.providerModels
    val modelsList = providerModels[activeProvider] ?: emptyList()
    var modelExpanded by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }

    var textInput by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val isAnalyzing = uiState.selectedRecord?.status == "PENDING"
    val useOcr = uiState.useOcr

    val currentOriginalText = uiState.currentOriginalText
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

    // Sleek, low-prominence Model Selection Pill Chip (Zen Style)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .animateContentSize(animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = "モデル: ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SumiInk.copy(alpha = 0.5f)
            )
            Box(modifier = Modifier.weight(1f, fill = false)) {
                Surface(
                    color = SumiInk.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .widthIn(max = 180.dp)
                        .clickable { modelExpanded = true }
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
                            color = SumiInk,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            onClick = {
                if (!isNavigating) {
                    isNavigating = true
                    navController.navigate("settings")
                }
            },
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

    OutlinedTextField(
        value = textInput,
        onValueChange = { 
            textInput = it 
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
            val key = viewModel.getApiKey(activeProvider)
            val url = viewModel.getApiUrl(activeProvider)
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
