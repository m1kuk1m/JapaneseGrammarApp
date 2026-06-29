package com.example.japanesegrammarapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkFilterChipsBar(
    searchQuery: String,
    sortOrder: BookmarkSortOrder,
    filterMode: BookmarkFilter,
    archiveFilter: ArchiveFilter,
    posCategories: List<String>,
    dateCategories: Set<Long>,
    selectedPosCategory: String?,
    selectedDateFilter: Long?,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (BookmarkSortOrder) -> Unit,
    onFilterModeChange: (BookmarkFilter) -> Unit,
    onArchiveFilterChange: (ArchiveFilter) -> Unit,
    onPosCategoryChange: (String?) -> Unit,
    onDateFilterChange: (Long?) -> Unit,
    onReset: () -> Unit,
    isDark: Boolean,
    showPosFilter: Boolean = true,
    showArchiveFilter: Boolean = true
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val activeFilterCount =
        (if (searchQuery.isNotBlank()) 1 else 0) +
            (if (filterMode != BookmarkFilter.ALL) 1 else 0) +
            (if (showPosFilter && selectedPosCategory != null) 1 else 0) +
            (if (selectedDateFilter != null) 1 else 0) +
            (if (showArchiveFilter && archiveFilter != ArchiveFilter.ALL) 1 else 0)

    Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
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
                .padding(top = 2.dp, bottom = 4.dp)
        ) {
            BookmarkFilterChip(
                label = sortOrder.displayName(),
                isSelected = true,
                onClick = {
                    onSortOrderChange(
                        if (sortOrder == BookmarkSortOrder.NEWEST_FIRST) {
                            BookmarkSortOrder.OLDEST_FIRST
                        } else {
                            BookmarkSortOrder.NEWEST_FIRST
                        }
                    )
                }
            )

            if (showArchiveFilter && archiveFilter != ArchiveFilter.ALL) {
                BookmarkFilterChip(
                    label = archiveFilter.displayName(),
                    isSelected = true,
                    onClick = { showFilterSheet = true }
                )
            }

            if (filterMode != BookmarkFilter.ALL) {
                BookmarkFilterChip(
                    label = filterMode.displaySummary(
                        selectedPosCategory = selectedPosCategory,
                        selectedDateFilter = selectedDateFilter
                    ),
                    isSelected = true,
                    onClick = { showFilterSheet = true }
                )
            }

            FilterChip(
                selected = activeFilterCount > 0,
                onClick = { showFilterSheet = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.bookmark_filter_open),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(Modifier.height(2.dp))
    }

    if (showFilterSheet) {
        BookmarkFiltersSheet(
            sortOrder = sortOrder,
            filterMode = filterMode,
            archiveFilter = archiveFilter,
            posCategories = posCategories,
            dateCategories = dateCategories,
            selectedPosCategory = selectedPosCategory,
            selectedDateFilter = selectedDateFilter,
            onSortOrderChange = onSortOrderChange,
            onFilterModeChange = onFilterModeChange,
            onArchiveFilterChange = onArchiveFilterChange,
            onPosCategoryChange = onPosCategoryChange,
            onDateFilterChange = onDateFilterChange,
            onDismiss = { showFilterSheet = false },
            onReset = onReset,
            isDark = isDark,
            showPosFilter = showPosFilter,
            showArchiveFilter = showArchiveFilter
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkFiltersSheet(
    sortOrder: BookmarkSortOrder,
    filterMode: BookmarkFilter,
    archiveFilter: ArchiveFilter,
    posCategories: List<String>,
    dateCategories: Set<Long>,
    selectedPosCategory: String?,
    selectedDateFilter: Long?,
    onSortOrderChange: (BookmarkSortOrder) -> Unit,
    onFilterModeChange: (BookmarkFilter) -> Unit,
    onArchiveFilterChange: (ArchiveFilter) -> Unit,
    onPosCategoryChange: (String?) -> Unit,
    onDateFilterChange: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    isDark: Boolean,
    showPosFilter: Boolean,
    showArchiveFilter: Boolean
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.bookmark_filter_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = sumiInk
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.bookmark_filter_reset))
                }
            }

            FilterSectionTitle(text = stringResource(R.string.bookmark_filter_sort))
            HorizontalFilterOptions {
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

            if (showArchiveFilter) {
                FilterSectionTitle(text = stringResource(R.string.bookmark_filter_scope))
                HorizontalFilterOptions {
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

            FilterSectionTitle(text = stringResource(R.string.bookmark_filter_grouping))
            HorizontalFilterOptions {
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
                
                val dateLabel = if (filterMode == BookmarkFilter.BY_DATE && selectedDateFilter != null) {
                    formatBookmarkDate(selectedDateFilter)
                } else {
                    stringResource(R.string.filter_by_date)
                }
                BookmarkFilterChip(
                    label = dateLabel,
                    isSelected = filterMode == BookmarkFilter.BY_DATE,
                    onClick = { showDatePicker = true }
                )
            }

            if (filterMode == BookmarkFilter.BY_POS && posCategories.isNotEmpty() && showPosFilter) {
                FilterSectionTitle(text = stringResource(R.string.bookmark_filter_details))
                HorizontalFilterOptions {
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

            if (showDatePicker) {
                val initialSelectedDate = selectedDateFilter ?: if (dateCategories.isNotEmpty()) dateCategories.maxOrNull() else null
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialSelectedDate,
                    selectableDates = remember(dateCategories) {
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return dateCategories.contains(utcTimeMillis)
                            }
                        }
                    }
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onFilterModeChange(BookmarkFilter.BY_DATE)
                                onDateFilterChange(datePickerState.selectedDateMillis)
                                showDatePicker = false
                            }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun HorizontalFilterOptions(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        content = content
    )
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
        modifier = Modifier.heightIn(min = 34.dp),
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
        modifier = Modifier.heightIn(min = 34.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = backgroundColor,
            containerColor = backgroundColor.copy(alpha = 0.4f),
            labelColor = labelColor.copy(alpha = 0.7f),
            selectedLabelColor = labelColor
        )
    )
}

@Composable
private fun BookmarkSortOrder.displayName(): String = when (this) {
    BookmarkSortOrder.NEWEST_FIRST -> stringResource(R.string.sort_newest_first)
    BookmarkSortOrder.OLDEST_FIRST -> stringResource(R.string.sort_oldest_first)
}

@Composable
private fun ArchiveFilter.displayName(): String = when (this) {
    ArchiveFilter.ALL -> stringResource(R.string.filter_all)
    ArchiveFilter.UNARCHIVED -> stringResource(R.string.archive_filter_unarchived)
    ArchiveFilter.ARCHIVED -> stringResource(R.string.archive_filter_archived)
}

@Composable
private fun formatBookmarkDate(millis: Long): String {
    val formatter = remember {
        java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
    }
    return formatter.format(java.util.Date(millis))
}

@Composable
private fun BookmarkFilter.displaySummary(
    selectedPosCategory: String?,
    selectedDateFilter: Long?
): String = when (this) {
    BookmarkFilter.ALL -> stringResource(R.string.filter_all)
    BookmarkFilter.BY_POS -> selectedPosCategory?.let { bookmarkPosDisplayName(it) }
        ?: stringResource(R.string.filter_by_pos)
    BookmarkFilter.BY_DATE -> selectedDateFilter?.let { formatBookmarkDate(it) }
        ?: stringResource(R.string.filter_by_date)
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
