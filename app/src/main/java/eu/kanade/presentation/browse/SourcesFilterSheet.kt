package eu.kanade.presentation.browse

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.BrowseFilterSheet
import eu.kanade.presentation.browse.components.ContentTypeFilterSection
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourcesFilterSheet(
    state: SourcesFilterState,
    onDismissRequest: () -> Unit,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    onShowAllContentTypes: () -> Unit,
    onToggleContentType: (EntryType) -> Unit,
    onToggleUnspecifiedContentType: () -> Unit,
) {
    BrowseFilterSheet(
        title = stringResource(MR.strings.source_filter_title),
        onDismissRequest = onDismissRequest,
    ) { contentModifier ->
        RowList(fastScroll = true, modifier = contentModifier) {
            item(key = "content-types") {
                ContentTypeFilterSection(
                    filter = state.contentTypeFilter,
                    onShowAll = onShowAllContentTypes,
                    onToggleContentType = onToggleContentType,
                    onToggleUnspecified = onToggleUnspecifiedContentType,
                )
            }
            // Each language is a card: its toggle first, then its sources while it is on.
            state.items.forEach { (language, sources) ->
                val enabled = language in state.enabledLanguages
                val rowCount = if (enabled) sources.size + 1 else 1
                item(key = "source-filter-gap-$language") {
                    RowGroupGap()
                }
                item(
                    key = language,
                    contentType = "source-filter-header",
                ) {
                    RowGroupItem(
                        position = RowGroupPosition.of(0, rowCount),
                        modifier = Modifier.animateItem(),
                    ) {
                        SourcesFilterHeader(
                            language = language,
                            enabled = enabled,
                            onClickItem = onClickLanguage,
                        )
                    }
                }
                if (enabled) {
                    itemsIndexed(
                        items = sources,
                        key = { _, source -> "source-filter-${source.key()}" },
                        contentType = { _, _ -> "source-filter-item" },
                    ) { index, source ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index + 1, rowCount),
                            modifier = Modifier.animateItem(),
                        ) {
                            SourcesFilterItem(
                                source = source,
                                enabled = "${source.id}" !in state.disabledSources,
                                onClickItem = onClickSource,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesFilterHeader(
    language: String,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleRow(
        title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
        checked = enabled,
        onCheckedChange = { onClickItem(language) },
        modifier = modifier,
    )
}

@Composable
private fun SourcesFilterItem(
    source: Source,
    enabled: Boolean,
    onClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = false,
        onClickItem = { onClickItem(source) },
        action = {
            Checkbox(checked = enabled, onCheckedChange = null)
        },
    )
}
