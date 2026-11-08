package app.katari.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import app.katari.entry.interactions.book.document.reader.search.BookDocumentChapterSearch
import app.katari.entry.interactions.book.document.reader.search.BookDocumentReaderSearchBar
import app.katari.entry.interactions.source.EntryChildWebViewAction
import app.katari.entry.interactions.source.EntryChildWebViewActionsMenu
import app.katari.entry.interactions.source.EntryChildWebViewResolution
import app.katari.entry.viewer.chrome.ui.bars.ReaderChromeTopBar
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Bookmark
import app.katari.icons.materialsymbols.rounded.Search
import app.katari.icons.materialsymbols.roundedfilled.Bookmark
import app.katari.presentation.kit.button.IconButton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The book and chapter with Bookmark, Search and More, or the search bar while [search] is open. */
@Composable
internal fun BookDocumentReaderTopBar(
    state: BookDocumentReaderState,
    search: BookDocumentChapterSearch,
    onOpenSearch: () -> Unit,
    onStepSearch: (forward: Boolean) -> Unit,
    onSearchAllChapters: () -> Unit,
    onCloseSearch: () -> Unit,
    onToggleBookmark: () -> Unit,
    webViewActions: List<EntryChildWebViewAction>,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onClose: () -> Unit,
) {
    if (search.query != null) {
        BookDocumentReaderSearchBar(
            search = search,
            onStep = onStepSearch,
            onSearchAllChapters = onSearchAllChapters,
            onClose = onCloseSearch,
        )
        return
    }
    ReaderChromeTopBar(
        title = state.entryTitle,
        subtitle = state.window.current.name,
        navigateUp = onClose,
        actions = {
            val bookmarked = state.currentChapterId in state.bookmarkedChapterIds
            IconButton(
                icon = if (bookmarked) MaterialSymbols.RoundedFilled.Bookmark else MaterialSymbols.Rounded.Bookmark,
                contentDescription = stringResource(
                    if (bookmarked) MR.strings.action_remove_bookmark else MR.strings.action_bookmark,
                ),
                onClick = onToggleBookmark,
            )
            IconButton(
                icon = MaterialSymbols.Rounded.Search,
                contentDescription = stringResource(MR.strings.action_search),
                onClick = onOpenSearch,
            )
            EntryChildWebViewActionsMenu(
                resolution = state.childWebView,
                onAction = onChildWebViewAction,
                actions = webViewActions,
            )
        },
    )
}
