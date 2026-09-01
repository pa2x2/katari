package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.book.api.document.BookDocumentLinkTarget
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.presentation.core.util.clickableNoIndication

/** Owns the lazy item provider so logical chapter activation cannot invalidate unchanged rows. */
@Composable
internal fun BookDocumentViewerList(
    items: BookDocumentViewerDataset<EntryChapter>,
    state: LazyListState,
    selection: BookDocumentChapterSelection,
    chapterLoadState: (Long) -> BookDocumentChapterLoadState?,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    onTransitionRetry: (EntryChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = state,
        modifier = modifier.clickableNoIndication {
            selection.handleReaderTap(onReaderTap)
        },
    ) {
        items(items, key = { it.key }) { item ->
            BookDocumentViewerRow(
                item = item,
                transitionDirection = (item as? BookDocumentViewerItem.Transition)?.transition?.direction,
                loadState = (item as? BookDocumentViewerItem.Transition)?.transition?.to?.let {
                    chapterLoadState(it.id)
                },
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
                onTransitionRetry = onTransitionRetry,
            )
        }
    }
}
