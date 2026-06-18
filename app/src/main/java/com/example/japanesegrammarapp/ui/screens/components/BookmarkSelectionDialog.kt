package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg

@Composable
fun BookmarkSelectionDialog(
    titleResId: Int,
    confirmResId: Int,
    onDismiss: () -> Unit,
    onConfirm: (includeWords: Boolean, includeSentences: Boolean) -> Unit
) {
    var includeWords by remember { mutableStateOf(true) }
    var includeSentences by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WashiBg),
            border = BorderStroke(1.5.dp, SumiInk.copy(alpha = 0.15f))
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
                    Text(
                        text = stringResource(id = titleResId),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SumiInk
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = SumiInk.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Words Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { includeWords = !includeWords }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeWords,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = SumiInk,
                            uncheckedColor = SumiInk.copy(alpha = 0.5f),
                            checkmarkColor = WashiBg
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.option_words),
                        fontSize = 15.sp,
                        color = SumiInk
                    )
                }

                // Sentences Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { includeSentences = !includeSentences }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeSentences,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = SumiInk,
                            uncheckedColor = SumiInk.copy(alpha = 0.5f),
                            checkmarkColor = WashiBg
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.option_sentences),
                        fontSize = 15.sp,
                        color = SumiInk
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                    ) {
                        Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val canConfirm = includeWords || includeSentences

                    Button(
                        onClick = {
                            onConfirm(includeWords, includeSentences)
                        },
                        enabled = canConfirm,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SumiInk,
                            contentColor = WashiBg,
                            disabledContainerColor = SumiInk.copy(alpha = 0.12f),
                            disabledContentColor = SumiInk.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(
                            text = stringResource(id = confirmResId),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
