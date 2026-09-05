package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.viewer.EntryChildDirection
import tachiyomi.domain.entry.model.EntryChapter

/** Renders one stable-key row without invalidating unchanged prose when the chapter window moves. */
@Composable
internal fun BookDocumentViewerRow(
    item: BookDocumentViewerItem<EntryChapter>,
    transitionDirection: EntryChildDirection?,
    loadState: BookDocumentChapterLoadState?,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    onTransitionRetry: (EntryChapter) -> Unit,
) {
    when (item) {
        is BookDocumentViewerItem.Block -> {
            val selection = LocalBookDocumentChapterSelection.current
            if (selection?.allowsSelection(item.section.owner.id) != false) {
                BookDocumentViewerBlock(
                    item = item,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    onReaderTap = onReaderTap,
                )
            } else {
                DisableSelection {
                    BookDocumentViewerBlock(
                        item = item,
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        onReaderTap = onReaderTap,
                    )
                }
            }
        }
        is BookDocumentViewerItem.Transition -> DisableSelection {
            BookDocumentChapterTransition(
                transition = item.transition,
                direction = transitionDirection ?: item.transition.direction,
                loadState = loadState,
                onRetry = item.transition.to?.let { chapter -> { onTransitionRetry(chapter) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            )
        }
    }
}

@Composable
internal fun BookDocumentViewerBlock(
    item: BookDocumentViewerItem.Block<EntryChapter>,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
) {
    CompositionLocalProvider(
        LocalBookDocumentSectionKey provides item.section.key,
        LocalBookDocumentResourceLoader provides item.section.resourceLoader,
        LocalBookDocumentSelectionChapterId provides item.section.owner.id,
    ) {
        BookDocumentBlockRenderer(
            block = item.content,
            owningContent = item.section.document.document.content,
            sectionKey = item.section.key,
            resourceLoader = item.section.resourceLoader,
            onAnchorClick = { target -> onAnchorClick(item.section, target) },
            onExternalLinkClick = onExternalLinkClick,
            onReaderTap = onReaderTap,
            preserveTerminalSpacing = item.content.id != item.section.document.blocks.last().id,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }
}
