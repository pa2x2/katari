package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.table.BookDocumentTablePreparation
import mihon.entry.interactions.book.document.reader.transition.LocalBookDocumentChapterTransitionMode
import mihon.entry.interactions.book.document.reader.transition.isSeamlessWhenNeededBoundary
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
    val sections = remember(items.identity) { items.current + items.next + items.previous }
    val transitionMode = LocalBookDocumentChapterTransitionMode.current
    val preparedChapterIds = remember(items.identity) { sections.map { it.owner.id }.toSet() }
    BookDocumentTablePreparation(sections, modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().clickableNoIndication {
                selection.handleReaderTap(onReaderTap)
            },
        ) {
            items(items, key = { it.key }) { item ->
                val transition = (item as? BookDocumentViewerItem.Transition)?.transition
                val destination = transition?.to
                val isSeamless = transition != null && destination != null &&
                    isSeamlessWhenNeededBoundary(
                        transitionMode,
                        transition,
                        destination.id in preparedChapterIds,
                        chapterLoadState(destination.id),
                    )
                if (isSeamless) {
                    Spacer(modifier = Modifier.fillMaxWidth())
                } else {
                    BookDocumentViewerRow(
                        item = item,
                        transitionDirection = transition?.direction,
                        loadState = destination?.let { chapterLoadState(it.id) },
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        onReaderTap = onReaderTap,
                        onTransitionRetry = onTransitionRetry,
                    )
                }
            }
        }
    }
}
