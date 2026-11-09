package eu.kanade.presentation.browse

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.sheet.SheetSplitList
import eu.kanade.presentation.browse.components.BrowseFilterSheet
import eu.kanade.presentation.browse.components.ContentTypeFilterSection
import eu.kanade.presentation.browse.components.FilterSectionHeading
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionFilterSheet(
    state: ExtensionFilterState,
    onDismissRequest: () -> Unit,
    onReset: () -> Unit,
    onShowAllContentTypes: () -> Unit,
    onToggleContentType: (EntryType) -> Unit,
    onToggleUnspecifiedContentType: () -> Unit,
    onToggleStore: (String) -> Unit,
    onToggleLanguage: (String) -> Unit,
) {
    BrowseFilterSheet(
        title = stringResource(MR.strings.extension_filter_title),
        onDismissRequest = onDismissRequest,
        onReset = onReset,
        resetEnabled = state.activeFilterCount > 0,
    ) { contentModifier ->
        SheetSplitList(
            modifier = contentModifier,
            start = {
                item(key = "content-types") {
                    ContentTypeFilterSection(
                        filter = state.contentTypes,
                        onShowAll = onShowAllContentTypes,
                        onToggleContentType = onToggleContentType,
                        onToggleUnspecified = onToggleUnspecifiedContentType,
                    )
                }

                if (state.stores.stores.isNotEmpty()) {
                    item(key = "stores-heading") {
                        FilterSectionHeading(
                            title = stringResource(MR.strings.extensionStores),
                            summary = stringResource(MR.strings.extension_filter_stores_summary),
                        )
                    }

                    rowGroupItems(
                        items = state.stores.stores,
                        key = { it.indexUrl },
                    ) { store ->
                        ToggleRow(
                            title = store.name,
                            subtitle = store.indexUrl,
                            checked = state.stores.isEnabled(store),
                            onCheckedChange = { onToggleStore(store.indexUrl) },
                        )
                    }
                }
            },
            end = {
                item(key = "languages-heading") {
                    FilterSectionHeading(
                        title = stringResource(MR.strings.extension_filter_languages),
                        summary = stringResource(MR.strings.extension_filter_languages_summary),
                    )
                }

                rowGroupItems(
                    items = state.languages,
                    key = { it },
                ) { language ->
                    ToggleRow(
                        title = LocaleHelper.getSourceDisplayName(language, currentPlatformContext()),
                        checked = language in state.enabledLanguages,
                        onCheckedChange = { onToggleLanguage(language) },
                    )
                }
            },
        )
    }
}
