package eu.kanade.presentation.browse

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.BrowseFilterSheet
import eu.kanade.presentation.browse.components.ContentTypeFilterSection
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
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
        FastScrollLazyColumn(modifier = contentModifier) {
            item(key = "content-types") {
                ContentTypeFilterSection(
                    filter = state.contentTypeFilter,
                    onShowAll = onShowAllContentTypes,
                    onToggleContentType = onToggleContentType,
                    onToggleUnspecified = onToggleUnspecifiedContentType,
                )
            }
            item(key = "content-types-divider") {
                HorizontalDivider()
            }

            state.items.forEach { (language, sources) ->
                val enabled = language in state.enabledLanguages
                item(
                    key = language,
                    contentType = "source-filter-header",
                ) {
                    SourcesFilterHeader(
                        modifier = Modifier.animateItemFastScroll(),
                        language = language,
                        enabled = enabled,
                        onClickItem = onClickLanguage,
                    )
                }
                if (enabled) {
                    items(
                        items = sources,
                        key = { "source-filter-${it.key()}" },
                        contentType = { "source-filter-item" },
                    ) { source ->
                        SourcesFilterItem(
                            modifier = Modifier.animateItemFastScroll(),
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

@Composable
private fun SourcesFilterHeader(
    language: String,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
        checked = enabled,
        onCheckedChanged = { onClickItem(language) },
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
