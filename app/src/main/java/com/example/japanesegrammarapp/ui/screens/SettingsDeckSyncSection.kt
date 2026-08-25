package com.example.japanesegrammarapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.SettingsUiState
import com.example.japanesegrammarapp.ui.SettingsViewModel

@Composable
fun SettingsDeckSyncSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val isRunning = uiState.isDeckSyncServiceRunning
    val settings = uiState.deckSyncSettings
    val ip = if (uiState.deckSyncIpAddress.isNotBlank()) uiState.deckSyncIpAddress else "127.0.0.1"
    val port = settings.port
    val serverUrl = "http://$ip:$port"

    val statusGreen = Color(0xFF2E7D32)
    val statusGreenBg = Color(0xFF4CAF50).copy(alpha = 0.12f)

    var showPortDialog by remember { mutableStateOf(false) }
    var guideExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshDeckSyncIpAddress(context)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Group 1: Service Status
        SettingsGroup(title = stringResource(R.string.deck_sync_group_status)) {
            // Main Service Switch Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleDeckSyncService(context) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else SumiInk.copy(alpha = 0.06f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = if (isRunning) MaterialTheme.colorScheme.primary else SumiInk.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.deck_sync_service_title),
                            fontSize = 15.sp,
                            color = SumiInk,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) statusGreen else SumiInk.copy(alpha = 0.35f))
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRunning) stringResource(R.string.deck_sync_status_running)
                        else stringResource(R.string.deck_sync_status_stopped),
                        fontSize = 12.sp,
                        color = if (isRunning) statusGreen else SumiInk.copy(alpha = 0.5f)
                    )
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = { viewModel.toggleDeckSyncService(context) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                )
            }

            // Server Address Card (when running)
            AnimatedVisibility(
                visible = isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    SettingsDivider(horizontalPadding = 0.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SumiInk.copy(alpha = 0.04f))
                            .border(1.dp, SumiInk.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lan,
                                    contentDescription = null,
                                    tint = SumiInk.copy(alpha = 0.5f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.deck_sync_server_address),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SumiInk.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = serverUrl,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SumiInk,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("YomiLLM Address", serverUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.deck_sync_copied_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.deck_sync_copy),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Group 2: Pairing & Security
        SettingsGroup(title = stringResource(R.string.deck_sync_group_security)) {
            // mDNS Broadcasting Status (2-tier responsive layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = SumiInk.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.deck_sync_mdns_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = SumiInk
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isRunning) statusGreenBg else SumiInk.copy(alpha = 0.06f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) statusGreen else SumiInk.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isRunning) stringResource(R.string.deck_sync_mdns_broadcasting)
                            else stringResource(R.string.deck_sync_mdns_idle),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isRunning) statusGreen else SumiInk.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.deck_sync_mdns_desc),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = SumiInk.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 34.dp)
                )
            }

            SettingsDivider()

            // 4-Digit Security PIN
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = SumiInk.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.deck_sync_pin_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = SumiInk
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.deck_sync_pin_desc),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = SumiInk.copy(alpha = 0.55f),
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            val newPin = viewModel.regenerateDeckSyncPin()
                            Toast.makeText(
                                context,
                                context.getString(R.string.deck_sync_pin_regenerated_toast, newPin),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.deck_sync_pin_regenerate), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PIN Code Cards with Tap-to-Copy
                val pin = settings.pin.padEnd(4, '0')
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("YomiLLM PIN", pin)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                context.getString(R.string.deck_sync_pin_copied_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pin.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit.toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = SumiInk.copy(alpha = 0.45f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.deck_sync_copy_pin_hint),
                            fontSize = 11.sp,
                            color = SumiInk.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            SettingsDivider()

            // Port Config Item
            SettingsItem(
                icon = Icons.Default.Router,
                title = stringResource(R.string.deck_sync_port_title),
                subtitle = stringResource(R.string.deck_sync_port_desc),
                onClick = { showPortDialog = true },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SumiInk.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = port.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        // Group 3: Setup Guide (Accordion Stepper)
        SettingsGroup(title = stringResource(R.string.deck_sync_group_guide)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { guideExpanded = !guideExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = SumiInk.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.deck_sync_guide_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = SumiInk
                    )
                }

                Icon(
                    imageVector = if (guideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SumiInk.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(
                visible = guideExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    SettingsDivider(horizontalPadding = 0.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val steps = listOf(
                        stringResource(R.string.deck_sync_step_1),
                        stringResource(R.string.deck_sync_step_2),
                        stringResource(R.string.deck_sync_step_3),
                        stringResource(R.string.deck_sync_step_4),
                        stringResource(R.string.deck_sync_step_5)
                    )

                    steps.forEachIndexed { index, stepText ->
                        val isLast = index == steps.size - 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(22.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!isLast) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.5.dp)
                                            .height(24.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = stepText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = SumiInk.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = if (!isLast) 8.dp else 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Port Editor Dialog
    if (showPortDialog) {
        var portInput by remember { mutableStateOf(port.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.deck_sync_edit_port_title),
                    fontWeight = FontWeight.Bold,
                    color = SumiInk
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.deck_sync_edit_port_desc),
                        fontSize = 13.sp,
                        color = SumiInk.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            portInput = clean
                            val num = clean.toIntOrNull()
                            isError = num == null || num !in 1024..65535
                        },
                        label = { Text("Port (1024-65535)") },
                        isError = isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = portInput.toIntOrNull()
                        if (num != null && num in 1024..65535) {
                            viewModel.setDeckSyncPort(num, context)
                            showPortDialog = false
                        }
                    },
                    enabled = !isError && portInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsDivider(horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Divider(
        color = SumiInk.copy(alpha = 0.08f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = horizontalPadding)
    )
}


