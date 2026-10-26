package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

/** A sheet listing [sources] with their icon and language, searchable by name, that hands back the one picked. */
@Composable
fun SourcePickerSheet(
    title: String,
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
            SheetHeader(title = title)
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(MR.strings.action_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            )
            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(top = KatariTheme.spacing.s, bottom = KatariTheme.spacing.m),
            ) {
                rowGroupItems(filteredSources, key = Source::id) { source ->
                    BaseSourceItem(
                        source = source,
                        onClickItem = { onSelect(source) },
                    )
                }
            }
        }
    }
}
