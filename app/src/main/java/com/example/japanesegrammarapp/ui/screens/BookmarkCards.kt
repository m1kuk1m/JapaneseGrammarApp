package com.example.japanesegrammarapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.effectivePosCategory
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import com.example.japanesegrammarapp.ui.screens.components.DictionarySearchControls
import com.example.japanesegrammarapp.ui.theme.ZenColors
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BookmarkCard(
    bookmark: BookmarkedSegmentDomain,
    isExpanded: Boolean,
    isPendingDelete: Boolean,
    isDark: Boolean,
    uiPreferencesRepository: UiPreferencesRepository,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onNavigateToSource: () -> Unit,
    onToggleArchive: () -> Unit,
    onPlayTts: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val posCat = bookmark.effectivePosCategory
    val chipBg = ZenThemeColors.getChipColor(posCat, isDark)

    val borderColor by animateColorAsState(
        targetValue = when {
            isPendingDelete -> Color(0xFFD32F2F).copy(alpha = 0.7f)
            isExpanded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else -> sumiInk.copy(alpha = 0.08f)
        },
        animationSpec = tween(250),
        label = "cardBorder"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (isExpanded) 4.dp else 1.dp,
        label = "cardElevation"
    )
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "expandArrow"
    )

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = cardElevation,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = bookmark.segmentText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = sumiInk,
                            modifier = Modifier.align(Alignment.Bottom)
                        )

                        val displayReading = bookmark.reading
                        if (!displayReading.isNullOrBlank() && displayReading != bookmark.segmentText) {
                            Text(
                                text = displayReading,
                                fontSize = 12.sp,
                                color = sumiInk.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .align(Alignment.Bottom)
                            )
                        }
                    }

                    if (!bookmark.partOfSpeech.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Surface(color = chipBg, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = bookmark.partOfSpeech,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = sumiInk.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.flashcard_speak),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) {
                                stringResource(R.string.collapse)
                            } else {
                                stringResource(R.string.expand)
                            },
                            tint = sumiInk.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(expandRotation)
                        )
                    }
                    IconButton(onClick = onNavigateToSource, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.view_source),
                            tint = sumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isExpanded && !bookmark.meaning.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = ZenColors.KuriAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bookmark.meaning,
                        fontSize = 13.sp,
                        color = sumiInk,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(10.dp))
                    Divider(color = sumiInk.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailRow(stringResource(R.string.dictionary_form), bookmark.dictionaryForm)
                        if (!bookmark.dictionaryFormReading.isNullOrBlank() &&
                            bookmark.dictionaryFormReading != bookmark.reading
                        ) {
                            DetailRow(
                                label = stringResource(R.string.dict_form_reading),
                                value = bookmark.dictionaryFormReading
                            )
                        }
                        DetailRow(stringResource(R.string.inflection), bookmark.inflection)
                        DetailRow(stringResource(R.string.role_in_sentence), bookmark.role)
                    }

                    if (bookmark.sourceText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = sumiInk.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.source_sentence_prefix, bookmark.sourceText),
                                fontSize = 12.sp,
                                color = sumiInk.copy(alpha = 0.5f),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val queryWord = bookmark.dictionaryForm.takeIf { !it.isNullOrBlank() } ?: bookmark.segmentText
                        DictionarySearchControls(
                            queryWord = queryWord,
                            uiPreferencesRepository = uiPreferencesRepository,
                            sumiInk = sumiInk,
                            surfaceColor = surfaceColor
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bookmark.bookmarkedAt.formatBookmarkDate(),
                            fontSize = 11.sp,
                            color = sumiInk.copy(alpha = 0.3f)
                        )

                        TextButton(
                            onClick = onToggleArchive,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (bookmark.isArchived) {
                                    stringResource(R.string.bookmark_restore)
                                } else {
                                    stringResource(R.string.bookmark_archive)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            DeleteConfirmationRow(
                visible = isPendingDelete,
                onCancelDelete = onCancelDelete,
                onConfirmDelete = onConfirmDelete
            )
        }
    }
}

@Composable
fun SentenceBookmarkCard(
    sentence: BookmarkedSentenceDomain,
    onNavigateToDetails: () -> Unit,
    onPlayTts: () -> Unit,
    onDelete: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val borderColor = if (showDeleteConfirm) {
        Color(0xFFD32F2F).copy(alpha = 0.7f)
    } else {
        sumiInk.copy(alpha = 0.08f)
    }

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sentence.originalText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = sumiInk,
                        lineHeight = 26.sp
                    )

                    if (!sentence.translation.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = sentence.translation,
                            fontSize = 14.sp,
                            color = sumiInk.copy(alpha = 0.65f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.flashcard_speak),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToDetails, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.view_details),
                            tint = sumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.unfavorite),
                            tint = sumiInk.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = sentence.bookmarkedAt.formatBookmarkDate(),
                fontSize = 11.sp,
                color = sumiInk.copy(alpha = 0.3f)
            )

            DeleteConfirmationRow(
                visible = showDeleteConfirm,
                onCancelDelete = { showDeleteConfirm = false },
                onConfirmDelete = {
                    onDelete()
                    showDeleteConfirm = false
                }
            )
        }
    }
}

@Composable
private fun DeleteConfirmationRow(
    visible: Boolean,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancelDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = sumiInk),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text(stringResource(R.string.cancel_delete), fontSize = 13.sp)
            }
            Button(
                onClick = onConfirmDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.delete_bookmark), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = sumiInk.copy(alpha = 0.45f),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = sumiInk,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun Long.formatBookmarkDate(): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(this))
}
