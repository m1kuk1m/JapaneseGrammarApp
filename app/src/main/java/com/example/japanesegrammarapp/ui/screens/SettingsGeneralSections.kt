package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.SettingsUiState

@Composable
fun SettingsAppearanceSection(
    uiState: SettingsUiState,
    onThemeModeChange: (String) -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground

    SettingsGroup(title = stringResource(R.string.appearance)) {
        var themeDropdownExpanded by remember { mutableStateOf(false) }
        SettingsItem(
            icon = Icons.Default.BrightnessMedium,
            title = stringResource(R.string.theme_mode),
            subtitle = when (uiState.themeMode) {
                "Light" -> stringResource(R.string.theme_light)
                "Dark" -> stringResource(R.string.theme_dark)
                else -> stringResource(R.string.theme_system)
            },
            onClick = { themeDropdownExpanded = true },
            trailingContent = {
                Box {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                    DropdownMenu(
                        expanded = themeDropdownExpanded,
                        onDismissRequest = { themeDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_system)) },
                            onClick = {
                                onThemeModeChange("System")
                                themeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_light)) },
                            onClick = {
                                onThemeModeChange("Light")
                                themeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_dark)) },
                            onClick = {
                                onThemeModeChange("Dark")
                                themeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        )

        Divider(color = sumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            icon = Icons.Default.Wallpaper,
            title = stringResource(R.string.wallpaper),
            subtitle = if (uiState.wallpaperUri.isNotBlank()) {
                stringResource(R.string.custom_image_set)
            } else {
                stringResource(R.string.none)
            },
            onClick = onPickWallpaper,
            trailingContent = {
                if (uiState.wallpaperUri.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = uiState.wallpaperUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onClearWallpaper,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_wallpaper),
                                tint = sumiInk.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    TextButton(onClick = onPickWallpaper) {
                        Text(stringResource(R.string.pick_wallpaper), color = sumiInk)
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsGeneralSection(
    uiState: SettingsUiState,
    currentLangLabel: String,
    totalTokensConsumed: Int,
    onUseOcrChange: (Boolean) -> Unit,
    onAutoNavigateResultChange: (Boolean) -> Unit,
    onImageTokenizerModeChange: (String) -> Unit,
    onShowTokenDialog: () -> Unit,
    onShowApiLogs: () -> Unit,
    onShowOcrDebug: () -> Unit,
    onShowPromptEditor: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    SettingsGroup(title = stringResource(R.string.general)) {
        var langDropdownExpanded by remember { mutableStateOf(false) }
        val displayLangLabel = if (currentLangLabel == "AUTO") {
            stringResource(R.string.language_auto)
        } else {
            currentLangLabel
        }

        SettingsItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.language),
            subtitle = displayLangLabel,
            onClick = { langDropdownExpanded = true },
            trailingContent = {
                Box {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                    DropdownMenu(
                        expanded = langDropdownExpanded,
                        onDismissRequest = { langDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.language_auto)) },
                            onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                                )
                                langDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags("en")
                                )
                                langDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("简体中文") },
                            onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags("zh")
                                )
                                langDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("日本語") },
                            onClick = {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags("ja")
                                )
                                langDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.ImageSearch,
            title = stringResource(R.string.local_ocr),
            subtitle = stringResource(R.string.local_ocr_desc),
            trailingContent = {
                Switch(
                    checked = uiState.useOcr,
                    onCheckedChange = onUseOcrChange,
                    colors = SettingsSwitchColors(onPrimaryColor, primaryColor, sumiInk)
                )
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.ocr_debug_title),
            subtitle = stringResource(R.string.ocr_debug_entry_desc),
            onClick = onShowOcrDebug,
            trailingContent = {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = sumiInk.copy(alpha = 0.4f))
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Launch,
            title = stringResource(R.string.auto_navigate_result),
            subtitle = stringResource(R.string.auto_navigate_result_desc),
            trailingContent = {
                Switch(
                    checked = uiState.autoNavigateResult,
                    onCheckedChange = onAutoNavigateResultChange,
                    colors = SettingsSwitchColors(onPrimaryColor, primaryColor, sumiInk)
                )
            }
        )

        var tokenizerModeDropdownExpanded by remember { mutableStateOf(false) }
        val currentModeLabel = when (uiState.imageTokenizerMode) {
            "repair" -> stringResource(R.string.image_tokenizer_mode_repair)
            else -> stringResource(R.string.image_tokenizer_mode_faithful)
        }

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.AutoFixHigh,
            title = stringResource(R.string.image_tokenizer_mode_title),
            subtitle = currentModeLabel,
            onClick = { tokenizerModeDropdownExpanded = true },
            trailingContent = {
                Box {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = sumiInk.copy(alpha = 0.5f))
                    DropdownMenu(
                        expanded = tokenizerModeDropdownExpanded,
                        onDismissRequest = { tokenizerModeDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(R.string.image_tokenizer_mode_faithful), fontWeight = FontWeight.Bold, color = sumiInk)
                                    Text(stringResource(R.string.image_tokenizer_mode_faithful_desc), fontSize = 11.sp, color = sumiInk.copy(alpha = 0.5f))
                                }
                            },
                            onClick = {
                                onImageTokenizerModeChange("faithful")
                                tokenizerModeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(R.string.image_tokenizer_mode_repair), fontWeight = FontWeight.Bold, color = sumiInk)
                                    Text(stringResource(R.string.image_tokenizer_mode_repair_desc), fontSize = 11.sp, color = sumiInk.copy(alpha = 0.5f))
                                }
                            },
                            onClick = {
                                onImageTokenizerModeChange("repair")
                                tokenizerModeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.DataUsage,
            title = stringResource(R.string.token_usage),
            subtitle = stringResource(R.string.token_usage_desc),
            onClick = onShowTokenDialog,
            trailingContent = {
                val formattedTotal = if (totalTokensConsumed >= 1000) {
                    String.format(java.util.Locale.US, "%.1fk", totalTokensConsumed / 1000.0)
                } else {
                    totalTokensConsumed.toString()
                }
                Text(text = formattedTotal, fontWeight = FontWeight.Bold, color = sumiInk)
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Code,
            title = stringResource(R.string.view_api_debug_logs),
            subtitle = stringResource(R.string.api_debug_logs_title),
            onClick = onShowApiLogs,
            trailingContent = {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = sumiInk.copy(alpha = 0.4f))
            }
        )

        SettingsDivider()

        SettingsItem(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.custom_prompts_title),
            subtitle = stringResource(R.string.custom_prompts_desc),
            onClick = onShowPromptEditor,
            trailingContent = {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = sumiInk.copy(alpha = 0.4f))
            }
        )
    }
}

@Composable
private fun SettingsDivider() {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Divider(color = sumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsSwitchColors(
    onPrimaryColor: androidx.compose.ui.graphics.Color,
    primaryColor: androidx.compose.ui.graphics.Color,
    sumiInk: androidx.compose.ui.graphics.Color
) = SwitchDefaults.colors(
    checkedThumbColor = onPrimaryColor,
    checkedTrackColor = primaryColor,
    uncheckedThumbColor = sumiInk.copy(alpha = 0.4f),
    uncheckedTrackColor = sumiInk.copy(alpha = 0.1f)
)
