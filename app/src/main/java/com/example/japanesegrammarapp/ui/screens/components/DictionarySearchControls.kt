package com.example.japanesegrammarapp.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import com.example.japanesegrammarapp.utils.DictionaryApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.DictionarySearchControls(
    queryWord: String,
    uiPreferencesRepository: UiPreferencesRepository,
    sumiInk: Color,
    surfaceColor: Color,
    useTextFieldAnchor: Boolean = true
) {
    val context = LocalContext.current
    var isDictMenuExpanded by remember { mutableStateOf(false) }
    var selectedDict by remember {
        val savedDict = uiPreferencesRepository.getLastDictionary(DictionaryApp.EUDIC.name)
        mutableStateOf(runCatching { DictionaryApp.valueOf(savedDict) }.getOrDefault(DictionaryApp.EUDIC))
    }

    if (useTextFieldAnchor) {
        ExposedDropdownMenuBox(
            expanded = isDictMenuExpanded,
            onExpandedChange = { isDictMenuExpanded = !isDictMenuExpanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = stringResource(selectedDict.nameResId),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDictMenuExpanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = sumiInk.copy(alpha = 0.15f),
                    focusedBorderColor = sumiInk.copy(alpha = 0.4f),
                    unfocusedTextColor = sumiInk,
                    focusedTextColor = sumiInk
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.menuAnchor().fillMaxWidth().heightIn(min = 48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
            ExposedDropdownMenu(
                expanded = isDictMenuExpanded,
                onDismissRequest = { isDictMenuExpanded = false }
            ) {
                DictionaryApp.values().forEach { dict ->
                    DictionaryMenuItem(
                        dict = dict,
                        onClick = {
                            selectedDict = dict
                            uiPreferencesRepository.saveLastDictionary(dict.name)
                            isDictMenuExpanded = false
                        }
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                color = surfaceColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, sumiInk.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { isDictMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(selectedDict.nameResId),
                        color = sumiInk,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = sumiInk.copy(alpha = 0.7f)
                    )
                }
            }
            androidx.compose.material3.DropdownMenu(
                expanded = isDictMenuExpanded,
                onDismissRequest = { isDictMenuExpanded = false }
            ) {
                DictionaryApp.values().forEach { dict ->
                    DictionaryMenuItem(
                        dict = dict,
                        onClick = {
                            selectedDict = dict
                            uiPreferencesRepository.saveLastDictionary(dict.name)
                            isDictMenuExpanded = false
                        }
                    )
                }
            }
        }
    }

    FilledTonalButton(
        onClick = { selectedDict.search(context, queryWord) },
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = sumiInk,
            contentColor = surfaceColor
        )
    ) {
        Text(text = stringResource(R.string.search_in_dict), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DictionaryMenuItem(
    dict: DictionaryApp,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(dict.nameResId), fontSize = 14.sp) },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )
}
