package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.ui.theme.ZenColors

@Composable
fun SourceSentenceDialog(
    bookmark: BookmarkedSegmentDomain,
    onDismiss: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bookmark.segmentText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = sumiInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = sumiInk.copy(alpha = 0.5f)
                        )
                    }
                }

                if (!bookmark.reading.isNullOrBlank() && bookmark.reading != bookmark.segmentText) {
                    Text(bookmark.reading, fontSize = 14.sp, color = sumiInk.copy(alpha = 0.5f))
                }

                if (!bookmark.meaning.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = ZenColors.KuriAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            bookmark.meaning,
                            fontSize = 14.sp,
                            color = sumiInk,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.target_sentence_header),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sumiInk.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = sumiInk.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        bookmark.sourceText.ifBlank { stringResource(R.string.no_source_sentence) },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = sumiInk,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (!bookmark.partOfSpeech.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                bookmark.partOfSpeech,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        bookmark.inflection?.let {
                            Text(it, fontSize = 12.sp, color = sumiInk.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.close), fontSize = 14.sp, color = sumiInk)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSettingsDialog(
    allBookmarks: List<BookmarkedSegmentDomain>,
    posCategories: List<String>,
    onDismiss: () -> Unit,
    onStartPractice: (mode: String, limit: Int, pos: String, scope: String) -> Unit
) {
    var studyMode by remember { mutableStateOf("ja_to_zh") }
    var cardLimit by remember { mutableStateOf(-1) }
    var selectedPos by remember { mutableStateOf("ALL") }
    var practiceScope by remember { mutableStateOf("unarchived") }

    val filteredCount = remember(practiceScope, allBookmarks) {
        when (practiceScope) {
            "archived" -> allBookmarks.count { it.isArchived }
            "all" -> allBookmarks.size
            else -> allBookmarks.count { !it.isArchived }
        }
    }

    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.flashcard_settings_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = sumiInk
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PracticeChipSection(
                    title = stringResource(R.string.flashcard_scope_label),
                    options = listOf(
                        "unarchived" to stringResource(R.string.flashcard_scope_unarchived),
                        "archived" to stringResource(R.string.flashcard_scope_archived),
                        "all" to stringResource(R.string.flashcard_scope_all)
                    ),
                    selectedValue = practiceScope,
                    onSelected = { practiceScope = it },
                    compact = true
                )

                PracticeChipSection(
                    title = stringResource(R.string.flashcard_mode_label),
                    options = listOf(
                        "ja_to_zh" to stringResource(R.string.flashcard_mode_ja_to_zh),
                        "zh_to_ja" to stringResource(R.string.flashcard_mode_zh_to_ja)
                    ),
                    selectedValue = studyMode,
                    onSelected = { studyMode = it }
                )

                PracticeChipSection(
                    title = stringResource(R.string.flashcard_count_label),
                    options = listOf(
                        "10" to "10",
                        "20" to "20",
                        "50" to "50",
                        "-1" to stringResource(R.string.flashcard_count_all, filteredCount)
                    ),
                    selectedValue = cardLimit.toString(),
                    onSelected = { cardLimit = it.toInt() }
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.flashcard_pos_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = sumiInk.copy(alpha = 0.5f)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            PracticeFilterChip(
                                selected = selectedPos == "ALL",
                                label = stringResource(R.string.flashcard_pos_all),
                                onClick = { selectedPos = "ALL" },
                                sumiInk = sumiInk,
                                surfaceColor = surfaceColor
                            )
                        }
                        items(posCategories) { cat ->
                            PracticeFilterChip(
                                selected = selectedPos == cat,
                                label = getBookmarkPosDisplayName(cat),
                                onClick = { selectedPos = cat },
                                sumiInk = sumiInk,
                                surfaceColor = surfaceColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartPractice(studyMode, cardLimit, selectedPos, practiceScope) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = sumiInk,
                    contentColor = surfaceColor
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.flashcard_start_btn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = sumiInk.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        },
        containerColor = surfaceColor,
        tonalElevation = 6.dp
    )
}

@Composable
private fun PracticeChipSection(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    compact: Boolean = false
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = sumiInk.copy(alpha = 0.5f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            options.forEach { (value, label) ->
                PracticeFilterChip(
                    selected = selectedValue == value,
                    label = label,
                    onClick = { onSelected(value) },
                    sumiInk = sumiInk,
                    surfaceColor = surfaceColor,
                    fontSize = if (compact) 10 else 12,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    sumiInk: Color,
    surfaceColor: Color,
    fontSize: Int = 12,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = fontSize.sp, maxLines = 1) },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = sumiInk,
            selectedLabelColor = surfaceColor,
            labelColor = sumiInk.copy(alpha = 0.7f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (selected) sumiInk else sumiInk.copy(alpha = 0.12f),
            borderWidth = if (selected) 1.5.dp else 1.dp
        ),
        modifier = modifier
    )
}

private val bookmarkPosNameKeys = mapOf(
    "NOUN" to R.string.pos_NOUN,
    "VERB" to R.string.pos_VERB,
    "ADJECTIVE" to R.string.pos_ADJECTIVE,
    "AUXILIARY" to R.string.pos_AUXILIARY,
    "PARTICLE" to R.string.pos_PARTICLE,
    "ADVERB" to R.string.pos_ADVERB,
    "CONJUNCTION" to R.string.pos_CONJUNCTION,
    "PRONOUN" to R.string.pos_PRONOUN,
    "INTERJECTION" to R.string.pos_INTERJECTION
)

@Composable
private fun getBookmarkPosDisplayName(category: String): String {
    val resId = bookmarkPosNameKeys[category] ?: R.string.pos_other
    return stringResource(resId)
}
