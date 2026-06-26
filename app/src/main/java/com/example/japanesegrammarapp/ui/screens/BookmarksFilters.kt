package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.japanesegrammarapp.ui.BookmarkSortOrder
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors

@Composable
fun BookmarkFilterChipsBar(
    searchQuery: String,
    sortOrder: BookmarkSortOrder,
    filterMode: BookmarkFilter,
    archiveFilter: ArchiveFilter,
    posCategories: List<String>,
    dateCategories: List<String>,
    selectedPosCategory: String?,
    selectedDateFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (BookmarkSortOrder) -> Unit,
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = sumiInk.copy(alpha = 0.45f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_history_search),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.bookmarks_search_hint),
                    fontSize = 13.sp
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = sumiInk.copy(alpha = 0.28f),
                unfocusedBorderColor = sumiInk.copy(alpha = 0.12f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 4.dp)
        ) {
            BookmarkFilterChip(
                label = stringResource(R.string.sort_newest_first),
                isSelected = sortOrder == BookmarkSortOrder.NEWEST_FIRST,
                onClick = { onSortOrderChange(BookmarkSortOrder.NEWEST_FIRST) }
            )
            BookmarkFilterChip(
                label = stringResource(R.string.sort_oldest_first),
                isSelected = sortOrder == BookmarkSortOrder.OLDEST_FIRST,
                onClick = { onSortOrderChange(BookmarkSortOrder.OLDEST_FIRST) }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            BookmarkFilterChip(
                label = stringResource(R.string.filter_all),
                isSelected = filterMode == BookmarkFilter.ALL,
                onClick = { onFilterModeChange(BookmarkFilter.ALL) }
            )
            if (showPosFilter) {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_by_pos),
                    isSelected = filterMode == BookmarkFilter.BY_POS,
                    onClick = { onFilterModeChange(BookmarkFilter.BY_POS) }
                )
            }
            BookmarkFilterChip(
                label = stringResource(R.string.filter_by_date),
                isSelected = filterMode == BookmarkFilter.BY_DATE,
                onClick = { onFilterModeChange(BookmarkFilter.BY_DATE) }
            )
        }

        if (showArchiveFilter) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_all),
                    isSelected = archiveFilter == ArchiveFilter.ALL,
                    onClick = { onArchiveFilterChange(ArchiveFilter.ALL) }
                )
                BookmarkFilterChip(
                    label = stringResource(R.string.archive_filter_unarchived),
                    isSelected = archiveFilter == ArchiveFilter.UNARCHIVED,
                    onClick = { onArchiveFilterChange(ArchiveFilter.UNARCHIVED) }
                )
                BookmarkFilterChip(
                    label = stringResource(R.string.archive_filter_archived),
                    isSelected = archiveFilter == ArchiveFilter.ARCHIVED,
                    onClick = { onArchiveFilterChange(ArchiveFilter.ARCHIVED) }
                )
            }
        }

        if (filterMode == BookmarkFilter.BY_POS && posCategories.isNotEmpty() && showPosFilter) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp)
            ) {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_all),
                    isSelected = selectedPosCategory == null,
                    onClick = { onPosCategoryChange(null) }
                )
                posCategories.forEach { cat ->
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

        if (filterMode == BookmarkFilter.BY_DATE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp)
            ) {
                BookmarkFilterChip(
                    label = stringResource(R.string.filter_all),
                    isSelected = selectedDateFilter == null,
                    onClick = { onDateFilterChange(null) }
                )
                dateCategories.forEach { dateCat ->
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
