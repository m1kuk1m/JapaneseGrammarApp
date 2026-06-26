package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import com.example.japanesegrammarapp.R

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SettingsTtsSection(
    selectedTtsProvider: String,
    onSelectedTtsProviderChange: (String) -> Unit,
    ttsUrls: Map<String, String>,
    onTtsUrlChange: (String, String) -> Unit,
    ttsKeys: Map<String, String>,
    savedTtsKeys: Map<String, String>,
    onTtsKeyChange: (String, String) -> Unit,
    ttsModels: Map<String, String>,
    onTtsModelChange: (String, String) -> Unit,
    ttsVoices: Map<String, String>,
    onTtsVoiceChange: (String, String) -> Unit,
    ttsRegions: Map<String, String>,
    onTtsRegionChange: (String, String) -> Unit,
    onSaveTtsSettings: () -> Unit,
    onRequestClearTtsKey: (String) -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val defaultKeyboardActions = KeyboardActions(onDone = {
        keyboardController?.hide()
        focusManager.clearFocus()
    })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, sumiInk.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            var ttsProviderExpanded by remember { mutableStateOf(false) }
            val ttsProviders = listOf("OpenAI", "Google", "Microsoft")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { ttsProviderExpanded = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.tts_provider),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = sumiInk
                )
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedTtsProvider, color = sumiInk.copy(alpha = 0.7f))
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = sumiInk.copy(alpha = 0.5f))
                    }
                    DropdownMenu(
                        expanded = ttsProviderExpanded,
                        onDismissRequest = { ttsProviderExpanded = false }
                    ) {
                        ttsProviders.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider) },
                                onClick = {
                                    onSelectedTtsProviderChange(provider)
                                    ttsProviderExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var ttsKeyVisible by remember { mutableStateOf(false) }

            if (selectedTtsProvider == "OpenAI" || selectedTtsProvider == "Google") {
                OutlinedTextField(
                    value = ttsUrls[selectedTtsProvider] ?: "",
                    onValueChange = { onTtsUrlChange(selectedTtsProvider, it) },
                    label = { Text(stringResource(R.string.base_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = defaultKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedTtsProvider == "Microsoft") {
                OutlinedTextField(
                    value = ttsRegions[selectedTtsProvider] ?: "",
                    onValueChange = { onTtsRegionChange(selectedTtsProvider, it) },
                    label = { Text(stringResource(R.string.tts_region_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = defaultKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = ttsKeys[selectedTtsProvider] ?: "",
                onValueChange = { onTtsKeyChange(selectedTtsProvider, it) },
                label = { Text(stringResource(R.string.api_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (ttsKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { ttsKeyVisible = !ttsKeyVisible }) {
                        Icon(if (ttsKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = defaultKeyboardActions
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val key = ttsKeys[selectedTtsProvider] ?: ""
                    if (key.isBlank() && savedTtsKeys[selectedTtsProvider]?.isNotBlank() == true) {
                        onRequestClearTtsKey(selectedTtsProvider)
                    } else {
                        onSaveTtsSettings()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_tts_settings))
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTtsProvider == "OpenAI") {
                OutlinedTextField(
                    value = ttsModels[selectedTtsProvider] ?: "",
                    onValueChange = { onTtsModelChange(selectedTtsProvider, it) },
                    label = { Text(stringResource(R.string.tts_model_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = defaultKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = ttsVoices[selectedTtsProvider] ?: "",
                onValueChange = { onTtsVoiceChange(selectedTtsProvider, it) },
                label = { Text(stringResource(R.string.tts_voice_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = defaultKeyboardActions
            )
        }
    }
}
