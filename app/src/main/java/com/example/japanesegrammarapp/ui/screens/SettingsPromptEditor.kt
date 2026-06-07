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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R

@Composable
fun SettingsPromptEditor(
    visible: Boolean,
    selectedPromptKey: String,
    promptText: String,
    onDismiss: () -> Unit,
    onPromptKeyChange: (String) -> Unit,
    onPromptTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onResetCurrent: () -> Unit,
    onResetAll: () -> Unit
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.reset_prompt))
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = onPrimaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_prompt), color = onPrimaryColor)
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
