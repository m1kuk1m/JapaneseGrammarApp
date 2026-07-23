package com.example.japanesegrammarapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.domain.model.ReasoningLevel
import com.example.japanesegrammarapp.domain.model.ComponentReasoningLevel
import com.example.japanesegrammarapp.ui.SettingsUiState

@Composable
fun SettingsApiPrioritySection(
    uiState: SettingsUiState,
    providers: List<String>,
    providerModels: Map<String, List<String>>,
    onActiveProviderChange: (String) -> Unit,
    onActiveModelChange: (String) -> Unit,
    onBackupProviderChange: (String) -> Unit,
    onBackupModelChange: (String) -> Unit,
    onUseBackupApiChange: (Boolean) -> Unit,
    onAutoRetryOnErrorChange: (Boolean) -> Unit,
    onFailoverToNextEndpointChange: (Boolean) -> Unit,
    onReasoningLevelChange: (ReasoningLevel) -> Unit,
    onComponentReasoningLevelChange: (String, ComponentReasoningLevel) -> Unit,
    onOpenCotDialog: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val activeProvider = uiState.activeProvider

    SettingsGroup(title = stringResource(R.string.api_config)) {
        var mainProviderExpanded by remember { mutableStateOf(false) }
        SettingsItem(
            icon = Icons.Default.Star,
            title = stringResource(R.string.main_api),
            subtitle = activeProvider,
            onClick = { mainProviderExpanded = true },
            trailingContent = {
                Box {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                    DropdownMenu(
                        expanded = mainProviderExpanded,
                        onDismissRequest = { mainProviderExpanded = false }
                    ) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider) },
                                onClick = {
                                    onActiveProviderChange(provider)
                                    mainProviderExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        )

        SettingsDivider()

        var mainModelExpanded by remember { mutableStateOf(false) }
        val mainModels = providerModels[activeProvider].orEmpty()
        SettingsItem(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.main_model),
            subtitle = uiState.activeModel.ifBlank { stringResource(R.string.unselected) },
            onClick = { mainModelExpanded = true },
            trailingContent = {
                Box {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                    DropdownMenu(
                        expanded = mainModelExpanded,
                        onDismissRequest = { mainModelExpanded = false }
                    ) {
                        mainModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    onActiveModelChange(model)
                                    mainModelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        )

        SettingsDivider()

        // 思考链 (CoT) 思考深度 - 弹窗式单独配置
        val strOff = stringResource(R.string.cot_level_off)
        val strAuto = stringResource(R.string.cot_level_auto)
        val strLow = stringResource(R.string.cot_level_low)
        val strMedium = stringResource(R.string.cot_level_medium)
        val strHigh = stringResource(R.string.cot_level_high)
        val strDesc = stringResource(R.string.cot_dialog_desc)

        val componentLabels = listOf("単語分割", "翻訳", "文節解析", "文法解説", "詳細文法解析")
        val levelCounts = componentLabels.map { uiState.componentReasoningLevels[it] ?: ComponentReasoningLevel.GLOBAL }
            .groupingBy { it }
            .eachCount()

        val summaryText = if (levelCounts.size == 1) {
            val singleLevel = levelCounts.keys.first()
            val levelLabel = when (singleLevel) {
                ComponentReasoningLevel.OFF -> strOff
                ComponentReasoningLevel.AUTO, ComponentReasoningLevel.GLOBAL -> strAuto
                ComponentReasoningLevel.LOW -> strLow
                ComponentReasoningLevel.MEDIUM -> strMedium
                ComponentReasoningLevel.HIGH -> strHigh
            }
            "$strDesc ($levelLabel)"
        } else {
            val details = levelCounts.entries.joinToString(", ") { (level, count) ->
                val name = when (level) {
                    ComponentReasoningLevel.OFF -> strOff
                    ComponentReasoningLevel.AUTO, ComponentReasoningLevel.GLOBAL -> strAuto
                    ComponentReasoningLevel.LOW -> strLow
                    ComponentReasoningLevel.MEDIUM -> strMedium
                    ComponentReasoningLevel.HIGH -> strHigh
                }
                "$name: $count"
            }
            details
        }

        SettingsItem(
            icon = Icons.Default.Psychology,
            title = stringResource(R.string.reasoning_level),
            subtitle = summaryText,
            onClick = onOpenCotDialog,
            trailingContent = {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = sumiInk.copy(alpha = 0.5f)
                )
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.auto_retry_on_error),
            subtitle = stringResource(R.string.auto_retry_on_error_desc),
            trailingContent = {
                Switch(
                    checked = uiState.autoRetryOnError,
                    onCheckedChange = onAutoRetryOnErrorChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onPrimaryColor,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = sumiInk.copy(alpha = 0.4f),
                        uncheckedTrackColor = sumiInk.copy(alpha = 0.1f)
                    )
                )
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.failover_to_next_endpoint),
            subtitle = stringResource(R.string.failover_to_next_endpoint_desc),
            trailingContent = {
                Switch(
                    checked = uiState.failoverToNextEndpoint,
                    onCheckedChange = onFailoverToNextEndpointChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onPrimaryColor,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = sumiInk.copy(alpha = 0.4f),
                        uncheckedTrackColor = sumiInk.copy(alpha = 0.1f)
                    )
                )
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Backup,
            title = stringResource(R.string.use_backup_api),
            subtitle = stringResource(R.string.use_backup_api_desc),
            trailingContent = {
                Switch(
                    checked = uiState.useBackupApi,
                    onCheckedChange = onUseBackupApiChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onPrimaryColor,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = sumiInk.copy(alpha = 0.4f),
                        uncheckedTrackColor = sumiInk.copy(alpha = 0.1f)
                    )
                )
            }
        )

        AnimatedVisibility(
            visible = uiState.useBackupApi,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
        ) {
            Column {
                SettingsDivider()

                var backupProviderExpanded by remember { mutableStateOf(false) }
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.backup_api),
                    subtitle = uiState.backupProvider,
                    onClick = { backupProviderExpanded = true },
                    trailingContent = {
                        Box {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                            DropdownMenu(
                                expanded = backupProviderExpanded,
                                onDismissRequest = { backupProviderExpanded = false }
                            ) {
                                providers.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider) },
                                        onClick = {
                                            onBackupProviderChange(provider)
                                            backupProviderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                SettingsDivider()

                var backupModelExpanded by remember { mutableStateOf(false) }
                val backupModels = providerModels[uiState.backupProvider].orEmpty()
                SettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.backup_model),
                    subtitle = uiState.backupModel.ifBlank { stringResource(R.string.unselected) },
                    onClick = { backupModelExpanded = true },
                    trailingContent = {
                        Box {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                            DropdownMenu(
                                expanded = backupModelExpanded,
                                onDismissRequest = { backupModelExpanded = false }
                            ) {
                                backupModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            onBackupModelChange(model)
                                            backupModelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsCredentialsSection(
    uiState: SettingsUiState,
    providers: List<String>,
    providerModels: Map<String, List<String>>,
    expandedProvider: String?,
    customModelInputs: Map<String, String>,
    onExpandedProviderChange: (String?) -> Unit,
    onAddEndpoint: (String) -> Unit,
    onEditEndpoint: (LlmEndpoint) -> Unit,
    onDeleteEndpoint: (LlmEndpoint) -> Unit,
    onToggleEndpoint: (String, LlmEndpoint, Boolean) -> Unit,
    onFetchModels: (String, LlmEndpoint) -> Unit,
    onCustomModelInputChange: (String, String) -> Unit,
    onAddCustomModel: (String, List<String>) -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Text(
        text = stringResource(R.string.credentials),
        style = MaterialTheme.typography.titleMedium,
        color = sumiInk,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )

    providers.forEach { provider ->
        val isExpanded = expandedProvider == provider

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
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedProviderChange(if (isExpanded) null else provider) }
                        .padding(16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        provider,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = sumiInk
                    )
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = sumiInk.copy(alpha = 0.5f)
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(160))
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        EndpointPoolSection(
                            endpoints = uiState.providerEndpoints[provider].orEmpty(),
                            endpointHasKeys = uiState.endpointHasKeys,
                            fetchingEndpointId = uiState.fetchingEndpointId,
                            onAddEndpoint = { onAddEndpoint(provider) },
                            onEditEndpoint = onEditEndpoint,
                            onDeleteEndpoint = onDeleteEndpoint,
                            onToggleEndpoint = { endpoint, enabled ->
                                onToggleEndpoint(provider, endpoint, enabled)
                            },
                            onFetchModels = { endpoint ->
                                onFetchModels(provider, endpoint)
                            }
                        )

                        Spacer(modifier = Modifier.size(12.dp))
                        val customModelInput = customModelInputs[provider].orEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = { onCustomModelInputChange(provider, it) },
                                label = { Text(stringResource(R.string.add_custom_model)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customModelInput.isNotBlank()) {
                                        val currentModels = providerModels[provider].orEmpty()
                                        if (!currentModels.contains(customModelInput)) {
                                            onAddCustomModel(provider, currentModels + customModelInput)
                                        }
                                        onCustomModelInputChange(provider, "")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = onPrimaryColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Divider(color = sumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun SettingsSliderItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = sumiInk.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = sumiInk
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
            androidx.compose.material3.Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = sumiInk.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}
