package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.BrowseFilterSheet
import eu.kanade.presentation.browse.components.ContentTypeFilterSection
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionFilterSheet(
    state: ExtensionFilterState,
    onDismissRequest: () -> Unit,
    onReset: () -> Unit,
    onShowAllContentTypes: () -> Unit,
    onToggleContentType: (EntryType) -> Unit,
    onToggleUnspecifiedContentType: () -> Unit,
    onToggleLanguage: (String) -> Unit,
) {
    BrowseFilterSheet(
        title = stringResource(MR.strings.extension_filter_title),
        onDismissRequest = onDismissRequest,
        onReset = onReset,
        resetEnabled = state.activeFilterCount > 0,
    ) { contentModifier ->
        LazyColumn(modifier = contentModifier) {
            item(key = "content-types") {
                ContentTypeFilterSection(
                    filter = state.contentTypes,
                    onShowAll = onShowAllContentTypes,
                    onToggleContentType = onToggleContentType,
                    onToggleUnspecified = onToggleUnspecifiedContentType,
                )
            }

            item(key = "divider") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            }

            item(key = "languages-heading") {
                Column(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.medium,
                    ),
                ) {
                    Text(
                        text = stringResource(MR.strings.extension_filter_languages),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(MR.strings.extension_filter_languages_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall),
                    )
                }
            }

            items(
                items = state.languages,
                key = { it },
            ) { language ->
                SwitchPreferenceWidget(
                    title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
                    checked = language in state.enabledLanguages,
                    onCheckedChanged = { onToggleLanguage(language) },
                )
            }
        }
    }
}
