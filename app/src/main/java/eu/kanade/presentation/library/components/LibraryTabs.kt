package eu.kanade.presentation.library.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryPageTab
import eu.kanade.tachiyomi.ui.library.displayTitle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun LibraryTabs(
    tabs: List<LibraryPageTab>,
    selectedTabId: String?,
    showItemCounts: Boolean,
    getItemCountForTab: (LibraryPageTab) -> Int?,
    onTabItemClick: (LibraryPageTab) -> Unit,
) {
    val selectedTabIndex = tabs.indexOfFirst { it.id == selectedTabId }
        .coerceAtLeast(0)
        .coerceAtMost(tabs.lastIndex.coerceAtLeast(0))
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabItemClick(tab) },
                text = {
                    TabText(
                        text = tab.displayTitle(stringResource(MR.strings.label_default)),
                        badgeCount = if (showItemCounts) getItemCountForTab(tab) else null,
                    )
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
