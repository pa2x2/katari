package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.library.LibraryPageTab
import eu.kanade.tachiyomi.ui.library.displayTitle
import mihon.presentation.kit.control.ConnectedToggleRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One chip row per grouping level below the primary tabs. */
@Composable
internal fun LibraryGroupChipRows(
    levels: List<LibraryGroupChipLevel>,
    showItemCounts: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        levels.forEach { level ->
            LibraryGroupChipRow(
                level = level,
                showItemCounts = showItemCounts,
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
) {
    val defaultCategoryTitle = stringResource(MR.strings.label_default)
    ConnectedToggleRow(
        options = level.tabs,
        key = LibraryPageTab::id,
        selected = { it.id == level.selectedTabId },
        onClick = level.onSelect,
        label = { tab ->
            val title = tab.displayTitle(defaultCategoryTitle)
            if (showItemCounts && tab.itemCount != null) "$title ${tab.itemCount}" else title
        },
    )
}
