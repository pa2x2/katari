package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibraryPageTab
import eu.kanade.tachiyomi.ui.library.displayTitle
import mihon.presentation.kit.navigation.PillTab
import mihon.presentation.kit.navigation.PillTabRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun LibraryTabs(
    tabs: List<LibraryPageTab>,
    selectedTabId: String?,
    showItemCounts: Boolean,
    onTabItemClick: (LibraryPageTab) -> Unit,
) {
    val selectedTabIndex = tabs.indexOfFirst { it.id == selectedTabId }
        .coerceAtLeast(0)
        .coerceAtMost(tabs.lastIndex.coerceAtLeast(0))
    PillTabRow(count = tabs.size, selectedIndex = selectedTabIndex) { index ->
        val tab = tabs[index]
        PillTab(
            title = tab.displayTitle(stringResource(MR.strings.label_default)),
            selected = selectedTabIndex == index,
            onClick = { onTabItemClick(tab) },
            count = if (showItemCounts) tab.itemCount else null,
        )
    }
}
