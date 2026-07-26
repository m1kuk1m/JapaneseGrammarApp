package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisModule
import com.example.japanesegrammarapp.domain.model.ComponentReasoningLevel
import com.example.japanesegrammarapp.ui.theme.ZenColors.KuriAmber
import com.example.japanesegrammarapp.ui.theme.ZenColors.MatchaGreen

@Composable
fun CotDepthDialog(
    componentReasoningLevels: Map<String, ComponentReasoningLevel>,
    onComponentReasoningLevelChange: (String, ComponentReasoningLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val strOff = stringResource(R.string.cot_level_off)
    val strAuto = stringResource(R.string.cot_level_auto)
    val strLow = stringResource(R.string.cot_level_low)
    val strMedium = stringResource(R.string.cot_level_medium)
    val strHigh = stringResource(R.string.cot_level_high)

    val strPresetAuto = stringResource(R.string.cot_preset_all_auto)
    val strPresetOff = stringResource(R.string.cot_preset_all_off)
    val strPresetLow = stringResource(R.string.cot_preset_all_low)
    val strPresetMedium = stringResource(R.string.cot_preset_all_medium)
    val strPresetHigh = stringResource(R.string.cot_preset_all_high)

    val components = AnalysisModule.entries.map { it.id to stringResource(it.displayNameRes()) }

    val levels = listOf(
        ComponentReasoningLevel.OFF to strOff,
        ComponentReasoningLevel.AUTO to strAuto,
        ComponentReasoningLevel.LOW to strLow,
        ComponentReasoningLevel.MEDIUM to strMedium,
        ComponentReasoningLevel.HIGH to strHigh
    )

    val presetItems = listOf(
        ComponentReasoningLevel.AUTO to strPresetAuto,
        ComponentReasoningLevel.OFF to strPresetOff,
        ComponentReasoningLevel.LOW to strPresetLow,
        ComponentReasoningLevel.MEDIUM to strPresetMedium,
        ComponentReasoningLevel.HIGH to strPresetHigh
    )

    fun applyPresetToAll(targetLevel: ComponentReasoningLevel) {
        components.forEach { (apiLabel, _) ->
            onComponentReasoningLevelChange(apiLabel, targetLevel)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.cot_dialog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.cot_dialog_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Presets Bar
                Text(
                    text = stringResource(R.string.cot_quick_presets),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetItems.forEach { (level, label) ->
                        FilterChip(
                            selected = false,
                            onClick = { applyPresetToAll(level) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // 5 Component Module Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    components.forEach { (apiLabel, displayTitle) ->
                        val currentLevel = componentReasoningLevels[apiLabel] ?: ComponentReasoningLevel.GLOBAL

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Badge showing current level
                                    val (badgeBg, badgeFg, badgeText) = when (currentLevel) {
                                        ComponentReasoningLevel.GLOBAL -> Triple(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.onSecondaryContainer,
                                            strAuto
                                        )
                                        ComponentReasoningLevel.OFF -> Triple(
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                            strOff
                                        )
                                        ComponentReasoningLevel.AUTO -> Triple(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                            strAuto
                                        )
                                        ComponentReasoningLevel.LOW -> Triple(
                                            MatchaGreen.copy(alpha = 0.2f),
                                            MatchaGreen,
                                            strLow
                                        )
                                        ComponentReasoningLevel.MEDIUM -> Triple(
                                            KuriAmber.copy(alpha = 0.2f),
                                            KuriAmber,
                                            strMedium
                                        )
                                        ComponentReasoningLevel.HIGH -> Triple(
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                            MaterialTheme.colorScheme.onTertiaryContainer,
                                            strHigh
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeFg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Segmented Control Buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    levels.forEach { (level, levelLabel) ->
                                        val isSelected = (currentLevel == level) ||
                                                (currentLevel == ComponentReasoningLevel.GLOBAL && level == ComponentReasoningLevel.AUTO)

                                        val activeColor = when (level) {
                                            ComponentReasoningLevel.OFF -> MaterialTheme.colorScheme.outline
                                            ComponentReasoningLevel.AUTO -> MaterialTheme.colorScheme.primary
                                            ComponentReasoningLevel.LOW -> MatchaGreen
                                            ComponentReasoningLevel.MEDIUM -> KuriAmber
                                            ComponentReasoningLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent
                                                )
                                                .clickable {
                                                    onComponentReasoningLevelChange(apiLabel, level)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = levelLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cot_done),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
