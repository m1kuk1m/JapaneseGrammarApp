package com.example.japanesegrammarapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.PromptPreset

@Composable
fun SettingsPromptEditor(
    visible: Boolean,
    selectedPromptKey: String,
    promptText: String,
    promptPresets: List<PromptPreset>,
    activePromptPresetId: String,
    onDismiss: () -> Unit,
    onPromptKeyChange: (String) -> Unit,
    onPromptTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onResetCurrent: () -> Unit,
    onResetAll: () -> Unit,
    onCreatePreset: (String, Boolean) -> Unit,
    onRenamePreset: (String, String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSelectPreset: (String) -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val washiBg = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = washiBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = sumiInk)
                    }
                    Text(
                        text = stringResource(R.string.prompt_editor_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = sumiInk
                    )
                    IconButton(onClick = { showResetAllConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.reset_all_prompts), tint = Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                PromptPresetSelector(
                    promptPresets = promptPresets,
                    activePromptPresetId = activePromptPresetId,
                    onSelectPreset = onSelectPreset,
                    onCreatePreset = onCreatePreset,
                    onRenamePreset = onRenamePreset,
                    onDeletePreset = onDeletePreset,
                    sumiInk = sumiInk,
                    surfaceColor = surfaceColor,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                PromptTypeSelector(
                    selectedPromptKey = selectedPromptKey,
                    onPromptKeyChange = onPromptKeyChange,
                    sumiInk = sumiInk,
                    surfaceColor = surfaceColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = promptText,
                    onValueChange = onPromptTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = sumiInk.copy(alpha = 0.15f),
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.reset_prompt), textAlign = TextAlign.Center)
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = onPrimaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_prompt), color = onPrimaryColor, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_prompt), fontWeight = FontWeight.Bold, color = sumiInk) },
            text = { Text(stringResource(R.string.prompt_reset_confirm), color = sumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetCurrent()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.reset_prompt), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = sumiInk)
                }
            },
            containerColor = surfaceColor
        )
    }

    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirm = false },
            title = { Text(stringResource(R.string.reset_all_prompts), fontWeight = FontWeight.Bold, color = sumiInk) },
            text = { Text(stringResource(R.string.prompt_reset_all_confirm), color = sumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAll()
                        showResetAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.reset_all_prompts), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = sumiInk)
                }
            },
            containerColor = surfaceColor
        )
    }
}

@Composable
private fun PromptTypeSelector(
    selectedPromptKey: String,
    onPromptKeyChange: (String) -> Unit,
    sumiInk: Color,
    surfaceColor: Color
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val promptKeys = listOf(
        "prompt_translation" to R.string.prompt_type_translation,
        "prompt_segments" to R.string.prompt_type_segments,
        "prompt_clauses" to R.string.prompt_type_clauses,
        "prompt_grammar" to R.string.prompt_type_grammar,
        "prompt_tokenizer" to R.string.prompt_type_tokenizer,
        "prompt_tokenizer_ocr" to R.string.prompt_type_tokenizer_ocr,
        "prompt_tokenizer_image" to R.string.prompt_type_tokenizer_image,
        "prompt_tokenizer_image_repair" to R.string.prompt_type_tokenizer_image_repair
    )
    val selectedLabelRes = promptKeys.find { it.first == selectedPromptKey }?.second ?: R.string.prompt_type_translation

    Text(
        text = stringResource(R.string.prompt_select_type),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = sumiInk.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, sumiInk.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
            .clickable { dropdownExpanded = true }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(selectedLabelRes), color = sumiInk, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = sumiInk.copy(alpha = 0.5f))
        }
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            promptKeys.forEach { (key, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onPromptKeyChange(key)
                        dropdownExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PromptPresetSelector(
    promptPresets: List<PromptPreset>,
    activePromptPresetId: String,
    onSelectPreset: (String) -> Unit,
    onCreatePreset: (String, Boolean) -> Unit,
    onRenamePreset: (String, String) -> Unit,
    onDeletePreset: (String) -> Unit,
    sumiInk: Color,
    surfaceColor: Color,
    primaryColor: Color,
    onPrimaryColor: Color
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val activePreset = promptPresets.find { it.id == activePromptPresetId } ?: promptPresets.firstOrNull()
    val isDefaultPreset = activePreset?.id == PromptPreset.DEFAULT_PRESET_ID

    Text(
        text = stringResource(R.string.prompt_presets),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = sumiInk.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(4.dp))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(surfaceColor, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, sumiInk.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                .clickable { dropdownExpanded = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDefaultPreset) stringResource(R.string.preset_default) else activePreset?.name ?: stringResource(R.string.preset_default), 
                    color = sumiInk, 
                    fontWeight = FontWeight.Medium
                )
                Icon(Icons.Default.KeyboardArrowDown, null, tint = sumiInk.copy(alpha = 0.5f))
            }
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                promptPresets.forEach { preset ->
                    val displayName = if (preset.id == PromptPreset.DEFAULT_PRESET_ID) stringResource(R.string.preset_default) else preset.name
                    DropdownMenuItem(
                        text = { Text(displayName) },
                        onClick = {
                            onSelectPreset(preset.id)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.new_preset), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { showRenameDialog = true },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isDefaultPreset
        ) {
            Text(stringResource(R.string.rename), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { showDeleteConfirmDialog = true },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isDefaultPreset,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
        ) {
            Text(stringResource(R.string.delete_preset), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }

    if (showCreateDialog) {
        PresetNameDialog(
            title = stringResource(R.string.new_preset),
            initialName = "",
            sumiInk = sumiInk,
            surfaceColor = surfaceColor,
            primaryColor = primaryColor,
            onPrimaryColor = onPrimaryColor,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name -> 
                onCreatePreset(name, true)
                showCreateDialog = false
            }
        )
    }

    if (showRenameDialog && activePreset != null) {
        PresetNameDialog(
            title = stringResource(R.string.rename_preset),
            initialName = activePreset.name,
            sumiInk = sumiInk,
            surfaceColor = surfaceColor,
            primaryColor = primaryColor,
            onPrimaryColor = onPrimaryColor,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name -> 
                onRenamePreset(activePreset.id, name)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog && activePreset != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_preset), fontWeight = FontWeight.Bold, color = sumiInk) },
            text = { Text(stringResource(R.string.delete_preset_confirm), color = sumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePreset(activePreset.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.delete_preset), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = sumiInk)
                }
            },
            containerColor = surfaceColor
        )
    }
}

@Composable
private fun PresetNameDialog(
    title: String,
    initialName: String,
    sumiInk: Color,
    surfaceColor: Color,
    primaryColor: Color,
    onPrimaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = sumiInk) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(stringResource(R.string.save_prompt), color = onPrimaryColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = sumiInk)
            }
        },
        containerColor = surfaceColor
    )
}
