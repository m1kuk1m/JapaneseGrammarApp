package com.example.japanesegrammarapp.ui.screens.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.WorkspaceUiState
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors

@Composable
fun WorkspaceInputForm(
    uiState: WorkspaceUiState,
    textInput: String,
    onTextInputChanged: (String) -> Unit,
    selectedImageUri: Uri?,
    onSelectedImageUriChanged: (Uri?) -> Unit,
    onModelSelected: (String) -> Unit,
    onStartAnalysis: (String, Uri?) -> Unit,
    onCancelAnalysis: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onPickImage: (Uri) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val PrimaryColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    val activeProvider = uiState.activeProvider
    val activeModel = uiState.activeModel
    val providerModels = uiState.providerModels
    val modelsList = providerModels[activeProvider] ?: emptyList()
    var modelExpanded by remember { mutableStateOf(false) }

    val isAnalyzing = uiState.selectedRecord?.status == AnalysisStatus.PENDING
    val useOcr = uiState.useOcr

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            onPickImage(sourceUri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() } // Clear focus when clicking outside
    ) {
        // Model Pill Selector
        Box {
            Surface(
                color = ZenThemeColors.pillBg(),
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clickable { modelExpanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFC5E2C6))) // MatchaGreen dot
                    Text(
                        text = activeModel.ifBlank { activeProvider },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SumiInk.copy(alpha = 0.8f),
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
                            onModelSelected(option)
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

        // Text Area (Borderless)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() }
        ) {
            if (textInput.isEmpty()) {
                Text(
                    text = stringResource(R.string.input_hint_elegant),
                    color = SumiInk.copy(alpha = 0.2f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            BasicTextField(
                value = textInput,
                onValueChange = { newValue ->
                    onTextInputChanged(newValue)
                    if (isAnalyzing) {
                        onCancelAnalysis()
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.analysis_interrupted),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = SumiInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 28.sp
                ),
                cursorBrush = SolidColor(PrimaryColor),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(top = 4.dp).fillMaxSize()) {
                        innerTextField()
                    }
                }
            )
        }

        // Selected Image Preview (if any, not using OCR)
        if (!useOcr && selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(64.dp)
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = stringResource(R.string.attached_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onSelectedImageUriChanged(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .offset(x = 6.dp, y = (-6).dp)
                        .background(SumiInk, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Media Actions (Camera/Gallery)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dashed attachment button (opens BottomSheet or just primary media action)
                // Let's implement Camera and Album directly as elegant small icons
                IconButton(
                    onClick = onNavigateToCamera,
                    modifier = Modifier
                        .size(48.dp)
                        .background(ZenThemeColors.buttonBg(), RoundedCornerShape(12.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = SumiInk.copy(alpha = 0.15f),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                ),
                                cornerRadius = CornerRadius(12.dp.toPx())
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.camera),
                        tint = SumiInk.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ZenThemeColors.buttonBg(), RoundedCornerShape(12.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = SumiInk.copy(alpha = 0.15f),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                ),
                                cornerRadius = CornerRadius(12.dp.toPx())
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = stringResource(R.string.album),
                        tint = SumiInk.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Primary Action Button (Circular)
            val canSubmit = (textInput.isNotBlank() || selectedImageUri != null) && !isAnalyzing
            IconButton(
                onClick = {
                    if (canSubmit) {
                        focusManager.clearFocus()
                        onStartAnalysis(textInput, selectedImageUri)
                        onSelectedImageUriChanged(null)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(if (canSubmit) SumiInk else SumiInk.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.start_parse),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}