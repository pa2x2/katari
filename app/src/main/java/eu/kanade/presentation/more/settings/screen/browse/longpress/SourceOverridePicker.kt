package eu.kanade.presentation.more.settings.screen.browse.longpress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.BaseSourceItem
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

/** Picks a source to give its own long-press priority. */
@Composable
internal fun SourceOverridePicker(
    sources: List<Source>,
    onSelect: (Source) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSources = remember(sources, query) {
        sources.filter { query.isBlank() || it.visualName.contains(query, ignoreCase = true) }
    }

    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.pref_browse_long_press_action_select_source))
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(MR.strings.action_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.l),
            )
            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(top = KatariTheme.spacing.s),
            ) {
                items(filteredSources, key = Source::id) { source ->
                    BaseSourceItem(
                        source = source,
                        onClickItem = { onSelect(source) },
                    )
                }
            }
        }
    }
}
