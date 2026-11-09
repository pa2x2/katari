package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.field.SearchField
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.sheet.SheetSplitList
import app.katari.presentation.kit.theme.KatariTheme
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
    val context = currentPlatformContext()
    var languageQuery by rememberSaveable { mutableStateOf("") }
    val languageNames = remember(state.languages, context) {
        state.languages.associateWith { LocaleHelper.getSourceDisplayName(it, context) }
    }
    val shownLanguages = remember(state.languages, languageNames, languageQuery) {
        val query = languageQuery.trim()
        if (query.isEmpty()) {
            state.languages
        } else {
            state.languages.filter {
                languageNames.getValue(it).contains(query, ignoreCase = true) || it.contains(query, ignoreCase = true)
            }
        }
    }
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

                item(key = "languages-search") {
                    SearchField(
                        query = languageQuery,
                        onQueryChange = { languageQuery = it },
                        placeholder = stringResource(MR.strings.extension_filter_find_language),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
                    )
                }

                rowGroupItems(
                    items = shownLanguages,
                    key = { it },
                ) { language ->
                    ToggleRow(
                        title = languageNames.getValue(language),
                        checked = language in state.enabledLanguages,
                        onCheckedChange = { onToggleLanguage(language) },
                    )
                }
            },
        )
    }
}
