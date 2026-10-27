package eu.kanade.presentation.browse.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.control.ConnectedToggleGroup
import mihon.presentation.kit.navigation.AppBarExtension
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.SearchToolbar
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    scrollBehavior: AppBarScrollBehavior,
) {
    Surface(container = SurfaceContainer.Surface) {
        Column {
            Box {
                SearchToolbar(
                    searchQuery = searchQuery,
                    onChangeSearchQuery = onChangeSearchQuery,
                    onSearch = onSearch,
                    onClickCloseSearch = navigateUp,
                    navigateUp = navigateUp,
                    scrollBehavior = scrollBehavior,
                )
                if (progress in 1..<total) {
                    LinearProgressIndicator(
                        progress = { progress / total.toFloat() },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                    )
                }
            }

            AppBarExtension {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(
                            start = KatariTheme.spacing.l,
                            end = KatariTheme.spacing.l,
                            top = KatariTheme.spacing.xs,
                            bottom = KatariTheme.spacing.s,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // TODO: make this UX better; it only applies when triggering a new search
                    if (!hideSourceFilter) {
                        ConnectedToggleGroup(
                            options = listOf(SourceFilter.PinnedOnly, SourceFilter.All),
                            selected = { it == sourceFilter },
                            onClick = onChangeSearchFilter,
                            label = {
                                when (it) {
                                    SourceFilter.PinnedOnly -> stringResource(MR.strings.pinned_sources)
                                    SourceFilter.All -> stringResource(MR.strings.all)
                                }
                            },
                        )
                    }

                    FilterChip(
                        label = stringResource(MR.strings.has_results),
                        selected = onlyShowHasResults,
                        onClick = onToggleResults,
                        icon = MaterialSymbols.Rounded.FilterList,
                    )
                }
            }
        }
    }
}
