package com.example.japanesegrammarapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.ArchiveFilter
import com.example.japanesegrammarapp.ui.BookmarkFilter

private val filterPosColors = mapOf(
    "NOUN" to Color(0xFFD3E0EA),
    "VERB" to Color(0xFFD4ECD5),
    "ADJECTIVE" to Color(0xFFF6E2CD),
    "AUXILIARY" to Color(0xFFE8D3EA),
    "PARTICLE" to Color(0xFFFDD4D8)
)

private val filterPosColorsDark = mapOf(
    "NOUN" to Color(0xFF1E2D3D),
    "VERB" to Color(0xFF1E3D24),
    "ADJECTIVE" to Color(0xFF3D2A1E),
    "AUXILIARY" to Color(0xFF2D1E3D),
    "PARTICLE" to Color(0xFF3D1E25)
)

@Composable
fun BookmarkFilterChipsBar(
    filterMode: BookmarkFilter,
    archiveFilter: ArchiveFilter,
    posCategories: List<String>,
    selectedPosCategory: String?,
    selectedDateFilter: String?,
    onFilterModeChange: (BookmarkFilter) -> Unit,
    onArchiveFilterChange: (ArchiveFilter) -> Unit,
    onPosCategoryChange: (String?) -> Unit,
    onDateFilterChange: (String?) -> Unit,
    isDark: Boolean
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_all),
                    isSelected = filterMode == BookmarkFilter.ALL,
                    onClick = { onFilterModeChange(BookmarkFilter.ALL) }
                )
            }
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_by_pos),
                    isSelected = filterMode == BookmarkFilter.BY_POS,
                    onClick = { onFilterModeChange(BookmarkFilter.BY_POS) }
                )
            }
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_by_date),
                    isSelected = filterMode == BookmarkFilter.BY_DATE,
                    onClick = { onFilterModeChange(BookmarkFilter.BY_DATE) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_all),
                    isSelected = archiveFilter == ArchiveFilter.ALL,
                    onClick = { onArchiveFilterChange(ArchiveFilter.ALL) }
                )
            }
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.archive_filter_unarchived),
                    isSelected = archiveFilter == ArchiveFilter.UNARCHIVED,
                    onClick = { onArchiveFilterChange(ArchiveFilter.UNARCHIVED) }
                )
            }
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.archive_filter_archived),
                    isSelected = archiveFilter == ArchiveFilter.ARCHIVED,
                    onClick = { onArchiveFilterChange(ArchiveFilter.ARCHIVED) }
                )
            }
        }

        AnimatedVisibility(visible = filterMode == BookmarkFilter.BY_POS && posCategories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                item {
                    BookmarkFilterChip(
                        label = stringResource(R.string.filter_all),
                        isSelected = selectedPosCategory == null,
                        onClick = { onPosCategoryChange(null) }
                    )
                }
                items(posCategories) { cat ->
                    val chipBg = (if (isDark) filterPosColorsDark[cat] else filterPosColors[cat])
                        ?: if (isDark) Color(0xFF2D2D2D) else Color(0xFFEFEFEF)
                    PosFilterChip(
                        label = bookmarkPosDisplayName(cat),
                        isSelected = selectedPosCategory == cat,
                        backgroundColor = chipBg,
                        labelColor = sumiInk,
                        onClick = { onPosCategoryChange(cat) }
                    )
                }
            }
        }

        AnimatedVisibility(visible = filterMode == BookmarkFilter.BY_DATE) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                item {
                    BookmarkFilterChip(
                        label = stringResource(R.string.filter_all),
                        isSelected = selectedDateFilter == null,
                        onClick = { onDateFilterChange(null) }
                    )
                }
                item {
                    BookmarkFilterChip(
                        label = stringResource(R.string.filter_today),
                        isSelected = selectedDateFilter == "today",
                        onClick = { onDateFilterChange("today") }
                    )
                }
                item {
                    BookmarkFilterChip(
                        label = stringResource(R.string.filter_week),
                        isSelected = selectedDateFilter == "week",
                        onClick = { onDateFilterChange("week") }
                    )
                }
                item {
                    BookmarkFilterChip(
                        label = stringResource(R.string.filter_older),
                        isSelected = selectedDateFilter == "older",
                        onClick = { onDateFilterChange("older") }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = sumiInk.copy(alpha = 0.12f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosFilterChip(
    label: String,
    isSelected: Boolean,
    backgroundColor: Color,
    labelColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = backgroundColor,
            containerColor = backgroundColor.copy(alpha = 0.4f),
            labelColor = labelColor.copy(alpha = 0.7f),
            selectedLabelColor = labelColor
        )
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
private fun bookmarkPosDisplayName(category: String): String {
    val resId = bookmarkPosNameKeys[category] ?: R.string.pos_other
    return stringResource(resId)
}
