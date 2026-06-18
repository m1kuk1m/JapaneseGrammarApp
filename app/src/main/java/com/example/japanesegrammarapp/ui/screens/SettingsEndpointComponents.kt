package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.LlmEndpoint

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun EndpointPoolSection(
    endpoints: List<LlmEndpoint>,
    fetchingEndpointId: String?,
    onAddEndpoint: () -> Unit,
    onEditEndpoint: (LlmEndpoint) -> Unit,
    onDeleteEndpoint: (LlmEndpoint) -> Unit,
    onToggleEndpoint: (LlmEndpoint, Boolean) -> Unit,
    onFetchModels: (LlmEndpoint) -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val now = System.currentTimeMillis()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.api_endpoints_manage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = sumiInk
            )
            OutlinedButton(
                onClick = onAddEndpoint,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        endpoints.forEach { endpoint ->
            val isCoolingDown = endpoint.cooldownUntilMs > now
            val isFetching = fetchingEndpointId == endpoint.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, sumiInk.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .background(sumiInk.copy(alpha = 0.025f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = endpoint.name.ifBlank { stringResource(R.string.endpoint_name) },
                            color = sumiInk,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = endpoint.baseUrl,
                            color = sumiInk.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = endpoint.enabled,
                        onCheckedChange = { onToggleEndpoint(endpoint, it) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (endpoint.enabled) {
                                stringResource(R.string.endpoint_enabled)
                            } else {
                                stringResource(R.string.endpoint_disabled)
                            },
                            color = if (endpoint.enabled) primaryColor else sumiInk.copy(alpha = 0.45f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = stringResource(
                                R.string.endpoint_priority_weight,
                                endpoint.priority,
                                endpoint.weight
                            ),
                            color = sumiInk.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        if (endpoint.consecutiveFailures > 0) {
                            Text(
                                text = stringResource(R.string.endpoint_failures, endpoint.consecutiveFailures),
                                color = Color(0xFFC62828),
                                fontSize = 11.sp
                            )
                        }
                        if (isCoolingDown) {
                            Text(
                                text = stringResource(
                                    R.string.endpoint_cooldown,
                                    formatEndpointDuration(endpoint.cooldownUntilMs - now)
                                ),
                                color = Color(0xFFE65100),
                                fontSize = 11.sp
                            )
                        }
                        endpoint.lastError?.takeIf { it.isNotBlank() }?.let { error ->
                            Text(
                                text = stringResource(R.string.endpoint_last_error, error),
                                color = sumiInk.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onFetchModels(endpoint) },
                            enabled = !isFetching && endpoint.enabled
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = primaryColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.fetch_models))
                            }
                        }
                        IconButton(onClick = { onEditEndpoint(endpoint) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.endpoint_edit))
                        }
                        IconButton(onClick = { onDeleteEndpoint(endpoint) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }

        if (endpoints.isEmpty()) {
            Button(
                onClick = onAddEndpoint,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = onPrimaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.endpoint_add), color = onPrimaryColor)
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun EndpointEditorDialog(
    provider: String,
    endpoint: LlmEndpoint?,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, apiKey: String, priority: Int, weight: Int) -> Unit
) {
    var name by remember(endpoint?.id) { mutableStateOf(endpoint?.name ?: "") }
    var baseUrl by remember(endpoint?.id) { mutableStateOf(endpoint?.baseUrl ?: "") }
    var apiKey by remember(endpoint?.id, initialKey) { mutableStateOf(initialKey) }
    var priorityText by remember(endpoint?.id) { mutableStateOf((endpoint?.priority ?: 0).toString()) }
    var weightText by remember(endpoint?.id) { mutableStateOf((endpoint?.weight ?: 1).toString()) }
    var keyVisible by remember { mutableStateOf(false) }
    val parsedPriority = priorityText.toIntOrNull()?.coerceAtLeast(0)
    val parsedWeight = weightText.toIntOrNull()?.coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (endpoint == null) {
                    stringResource(R.string.endpoint_add)
                } else {
                    stringResource(R.string.endpoint_edit)
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val defaultKeyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
            })
            Column {
                Text(provider, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.endpoint_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = defaultKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.base_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = defaultKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = defaultKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priorityText,
                        onValueChange = { priorityText = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.endpoint_priority)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = defaultKeyboardActions
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.endpoint_weight)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = defaultKeyboardActions
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        baseUrl.trim(),
                        apiKey.trim(),
                        parsedPriority ?: 0,
                        parsedWeight ?: 1
                    )
                },
                enabled = name.isNotBlank() &&
                    (baseUrl.isNotBlank() || endpoint != null) &&
                    parsedPriority != null &&
                    parsedWeight != null
            ) {
                Text(stringResource(R.string.endpoint_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatEndpointDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) + 999L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
