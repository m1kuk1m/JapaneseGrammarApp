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
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors

@Composable
fun BookmarkFilterChipsBar(
    filterMode: BookmarkFilter,
    archiveFilter: ArchiveFilter,
    posCategories: List<String>,
    dateCategories: List<String>,
    selectedPosCategory: String?,
    selectedDateFilter: String?,
    onFilterModeChange: (BookmarkFilter) -> Unit,
    onArchiveFilterChange: (ArchiveFilter) -> Unit,
    onPosCategoryChange: (String?) -> Unit,
    onDateFilterChange: (String?) -> Unit,
    isDark: Boolean,
    showPosFilter: Boolean = true,
    showArchiveFilter: Boolean = true
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
            if (showPosFilter) {
                item {
                    BookmarkFilterChip(
                        label = stringResource(R.string.filter_by_pos),
                        isSelected = filterMode == BookmarkFilter.BY_POS,
                        onClick = { onFilterModeChange(BookmarkFilter.BY_POS) }
                    )
                }
            }
            item {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_by_date),
                    isSelected = filterMode == BookmarkFilter.BY_DATE,
                    onClick = { onFilterModeChange(BookmarkFilter.BY_DATE) }
                )
            }
        }

        if (showArchiveFilter) {
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
        }

        AnimatedVisibility(visible = filterMode == BookmarkFilter.BY_POS && posCategories.isNotEmpty() && showPosFilter) {
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
                    val chipBg = ZenThemeColors.getChipColor(cat, isDark)
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
                items(dateCategories) { dateCat ->
                    val label = when (dateCat) {
                        "today" -> stringResource(R.string.filter_today)
                        "week" -> stringResource(R.string.filter_week)
                        else -> {
                            val parts = dateCat.split("/")
                            if (parts.size == 2) {
                                stringResource(R.string.year_month_format, parts[0], parts[1])
                            } else {
                                dateCat
                            }
                        }
                    }
                    BookmarkFilterChip(
                        label = label,
                        isSelected = selectedDateFilter == dateCat,
                        onClick = { onDateFilterChange(dateCat) }
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
    "INTERJECTION" to R.string.pos_INTERJECTION,
    "PRE_NOUN_ADJECTIVAL" to R.string.pos_PRE_NOUN_ADJECTIVAL,
    "SYMBOL" to R.string.pos_SYMBOL
)

@Composable
private fun bookmarkPosDisplayName(category: String): String {
    val resId = bookmarkPosNameKeys[category] ?: R.string.pos_other
    return stringResource(resId)
}
