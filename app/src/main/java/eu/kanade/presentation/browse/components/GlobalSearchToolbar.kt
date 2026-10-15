package eu.kanade.presentation.browse.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Keep
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.SearchToolbar
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.VerticalDivider
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

            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = KatariTheme.spacing.s),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // TODO: make this UX better; it only applies when triggering a new search
                if (!hideSourceFilter) {
                    FilterChip(
                        label = stringResource(MR.strings.pinned_sources),
                        selected = sourceFilter == SourceFilter.PinnedOnly,
                        onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
                        icon = MaterialSymbols.Rounded.Keep,
                    )
                    FilterChip(
                        label = stringResource(MR.strings.all),
                        selected = sourceFilter == SourceFilter.All,
                        onClick = { onChangeSearchFilter(SourceFilter.All) },
                        icon = MaterialSymbols.Rounded.DoneAll,
                    )

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = KatariTheme.spacing.s),
                    )
                }

                FilterChip(
                    label = stringResource(MR.strings.has_results),
                    selected = onlyShowHasResults,
                    onClick = onToggleResults,
                    icon = MaterialSymbols.Rounded.FilterList,
                )
            }

            HorizontalDivider()
        }
    }
}
