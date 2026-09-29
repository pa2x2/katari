package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryPageTab
import eu.kanade.tachiyomi.ui.library.displayTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One chip row per grouping level below the primary tabs; deeper levels get quieter chips. */
@Composable
internal fun LibraryGroupChipRows(
    levels: List<LibraryGroupChipLevel>,
    showItemCounts: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        levels.forEachIndexed { index, level ->
            LibraryGroupChipRow(
                level = level,
                showItemCounts = showItemCounts,
                prominent = index == 0,
            )
        }
    }
}

internal data class LibraryGroupChipLevel(
    val tabs: List<LibraryPageTab>,
    val selectedTabId: String?,
    val onSelect: (LibraryPageTab) -> Unit,
)

@Composable
private fun LibraryGroupChipRow(
    level: LibraryGroupChipLevel,
    showItemCounts: Boolean,
    prominent: Boolean,
) {
    val listState = rememberLazyListState()
    val selectedIndex = level.tabs.indexOfFirst { it.id == level.selectedTabId }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    val defaultCategoryTitle = stringResource(MR.strings.label_default)
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(level.tabs, key = { _, tab -> tab.id }) { index, tab ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { level.onSelect(tab) },
                label = {
                    Text(
                        text = tab.displayTitle(defaultCategoryTitle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showItemCounts && tab.itemCount != null) {
                        Text(
                            text = " ${tab.itemCount}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingIcon = if (selected && prominent) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                modifier = if (prominent) Modifier else Modifier.height(28.dp),
                shape = if (prominent) FilterChipDefaults.shape else MaterialTheme.shapes.large,
                colors = if (prominent) {
                    FilterChipDefaults.filterChipColors()
                } else {
                    FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                },
                border = if (prominent) {
                    FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
                } else {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent,
                    )
                },
            )
        }
    }
}
