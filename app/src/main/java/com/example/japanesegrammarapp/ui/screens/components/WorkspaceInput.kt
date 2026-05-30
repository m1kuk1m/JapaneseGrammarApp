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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.AppViewModel
import com.example.japanesegrammarapp.ui.WorkspaceUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceInputForm(
    uiState: WorkspaceUiState,
    viewModel: AppViewModel,
    navController: NavController,
    onNavigateToSettings: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val PrimaryColor = MaterialTheme.colorScheme.primary
    val OnPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val context = LocalContext.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    
    val activeProvider = uiState.activeProvider
    val activeModel = uiState.activeModel
    val providerModels = uiState.providerModels
    val modelsList = providerModels[activeProvider] ?: emptyList()
    var modelExpanded by remember { mutableStateOf(false) }

    var textInput by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val isAnalyzing = uiState.selectedRecord?.status == AnalysisStatus.PENDING
    val useOcr = uiState.useOcr

    val currentOriginalText = uiState.currentOriginalText
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun handleImageCaptured(uri: Uri) {
        if (useOcr) {
            coroutineScope.launch {
                val extracted = viewModel.extractTextFromImage(uri)
                textInput = extracted
                viewModel.setCurrentOriginalText(extracted)
                if (extracted.isNotBlank()) {
                    val key = viewModel.getApiKey(activeProvider)
                    val url = viewModel.getApiUrl(activeProvider)
                    viewModel.analyzeText(extracted, uri, activeProvider, activeModel.ifBlank { "default" }, url, key)
                }
            }
        } else {
            selectedImageUri = uri
        }
    }

    LaunchedEffect(currentOriginalText) {
        textInput = currentOriginalText
    }

    val navBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.getStateFlow<String?>("captured_image_uri", null)
            ?.collect { uriString ->
                if (!uriString.isNullOrBlank()) {
                    val uri = Uri.parse(uriString)
                    handleImageCaptured(uri)
                    navBackStackEntry.savedStateHandle["captured_image_uri"] = null
                }
            }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                var localUriToUse: Uri? = null
                val success = withContext(Dispatchers.IO) {
                    try {
                        val imagesDir = java.io.File(context.filesDir, "images")
                        if (!imagesDir.exists()) imagesDir.mkdirs()
                        val uniqueFileName = "gallery_${System.currentTimeMillis()}.jpg"
                        val galleryFile = java.io.File(imagesDir, uniqueFileName)
                        
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            java.io.FileOutputStream(galleryFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        
                        localUriToUse = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "com.example.japanesegrammarapp.fileprovider",
                            galleryFile
                        )
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
                if (success && localUriToUse != null) {
                    navController.navigate("camera?imageUri=${Uri.encode(localUriToUse.toString())}")
                } else {
                    navController.navigate("camera?imageUri=${Uri.encode(sourceUri.toString())}")
                }
            }
        }
    }

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
                text = stringResource(R.string.model_label),
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
                            text = activeModel.ifBlank { activeProvider },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.select),
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
                            text = { Text(stringResource(R.string.model_not_set), color = SumiInk.copy(alpha = 0.5f), fontSize = 13.sp) },
                            onClick = {
                                modelExpanded = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = textInput,
        onValueChange = { newValue ->
            textInput = newValue
            if (isAnalyzing) {
                val activeRecordId = uiState.selectedRecord?.id
                if (activeRecordId != null) {
                    viewModel.cancelAnalysis(activeRecordId)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.analysis_interrupted),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        placeholder = { Text(stringResource(R.string.input_hint), color = SumiInk.copy(alpha = 0.4f)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(8.dp),
        maxLines = 10,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            focusedLabelColor = PrimaryColor,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = PrimaryColor
        )
    )

    if (!useOcr && selectedImageUri != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
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
                    contentDescription = stringResource(R.string.attached_image),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.analysis_target_image), fontWeight = FontWeight.Bold, color = SumiInk, fontSize = 12.sp)
                }
                IconButton(onClick = { selectedImageUri = null }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFD32F2F))
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { navController.navigate("camera") }, 
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, PrimaryColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
        ) {
            Text(stringResource(R.string.camera_capture), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") }, 
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, PrimaryColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
        ) {
            Text(stringResource(R.string.pick_image), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            containerColor = PrimaryColor,
            contentColor = OnPrimaryColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Text(stringResource(R.string.start_analysis), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
