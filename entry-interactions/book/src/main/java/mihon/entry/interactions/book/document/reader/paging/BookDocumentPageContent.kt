package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerRow
import tachiyomi.domain.entry.model.EntryChapter

@Composable
internal fun BookDocumentPageContent(
    page: BookDocumentPage,
    loadStates: Map<Long, BookDocumentChapterLoadState>,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    onRetry: (EntryChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().then(
            if (page.scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
        ),
    ) {
        page.fragments.forEach { fragment ->
            BookDocumentPageFragmentContent(
                fragment,
                loadStates,
                onAnchorClick,
                onExternalLinkClick,
                onReaderTap,
                onRetry,
            )
        }
    }
}

@Composable
internal fun BookDocumentPageFragmentContent(
    fragment: BookDocumentPageFragment,
    loadStates: Map<Long, BookDocumentChapterLoadState>,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    onRetry: (EntryChapter) -> Unit,
) {
    val item = fragment.item
    val rendered = if (item is BookDocumentViewerItem.Block && item.content.content is BookDocumentBlockContent.Text) {
        item.copy(
            content = item.content.pageTextSlice(
                item.section.document.document.content.text,
                fragment.start,
                fragment.end,
            ),
        )
    } else {
        item
    }
    val transition = (item as? BookDocumentViewerItem.Transition)?.transition
    BookDocumentViewerRow(
        item = rendered,
        transitionDirection = transition?.direction,
        loadState = transition?.to?.id?.let(loadStates::get),
        onAnchorClick = onAnchorClick,
        onExternalLinkClick = onExternalLinkClick,
        onReaderTap = onReaderTap,
        onTransitionRetry = onRetry,
    )
}
