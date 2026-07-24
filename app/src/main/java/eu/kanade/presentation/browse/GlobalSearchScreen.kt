package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchItemCardRow
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchItem
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.entry.interactions.EntryCatalogueSourceInfo
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun GlobalSearchScreen(
    state: GlobalSearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getItem: @Composable (GlobalSearchItem) -> State<GlobalSearchItem>,
    onClickSource: (EntryCatalogueSourceInfo) -> Unit,
    onClickItem: (GlobalSearchItem) -> Unit,
    onLongClickItem: (GlobalSearchItem) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getItem = getItem,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<EntryCatalogueSourceInfo, GlobalSearchItemResult>,
    contentPadding: PaddingValues,
    getItem: @Composable (GlobalSearchItem) -> State<GlobalSearchItem>,
    onClickSource: (EntryCatalogueSourceInfo) -> Unit,
    onClickItem: (GlobalSearchItem) -> Unit,
    onLongClickItem: (GlobalSearchItem) -> Unit,
    fromSourceId: Long? = null,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = fromSourceId?.let {
                        "▶ ${source.name}".takeIf { source.id == fromSourceId }
                    } ?: source.name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.language),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        GlobalSearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is GlobalSearchItemResult.Success -> {
                            GlobalSearchItemCardRow(
                                titles = result.result,
                                getItem = getItem,
                                sourceItemOrientation = source.itemOrientation,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is GlobalSearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}
