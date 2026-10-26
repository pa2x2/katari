package mihon.entry.interactions.book.document.reader.search

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Search
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.navigation.OverlayBarRow
import mihon.presentation.kit.navigation.SearchToolbar
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The reader's top bar while searching: the query with a "1 of 2" counter, the previous and next match, and once
 * something is typed, Search all chapters on a row beneath.
 */
@Composable
internal fun BookDocumentReaderSearchBar(
    search: BookDocumentChapterSearch,
    onStep: (forward: Boolean) -> Unit,
    onSearchAllChapters: () -> Unit,
    onClose: () -> Unit,
) {
    val query = search.query.orEmpty()
    val matchCount = search.matches.size
    Column {
        SearchToolbar(
            searchQuery = query,
            onChangeSearchQuery = { value -> if (value == null) onClose() else search.changeQuery(value) },
            placeholderText = stringResource(MR.strings.book_reader_search_hint),
            onClickCloseSearch = onClose,
            matchCounter = if (query.isBlank()) {
                null
            } else {
                stringResource(MR.strings.book_reader_search_counter, search.currentIndex + 1, matchCount)
            },
            overlay = true,
            large = false,
            actions = {
                IconButton(
                    icon = MaterialSymbols.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(MR.strings.action_previous_match),
                    onClick = { onStep(false) },
                    enabled = matchCount > 0,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(MR.strings.action_next_match),
                    onClick = { onStep(true) },
                    enabled = matchCount > 0,
                )
            },
        )
        if (query.isNotBlank()) {
            OverlayBarRow(
                title = stringResource(MR.strings.book_reader_search_all_chapters),
                icon = MaterialSymbols.Rounded.Search,
                onClick = onSearchAllChapters,
            )
        }
    }
}
