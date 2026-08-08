package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.viewer.EntryChildDirection
import tachiyomi.domain.entry.model.EntryChapter

/** Renders one stable-key row without invalidating unchanged prose when the chapter window moves. */
@Composable
internal fun BookDocumentViewerRow(
    item: BookDocumentViewerItem<EntryChapter>,
    transitionDirection: EntryChildDirection?,
    loadState: BookDocumentChapterLoadState?,
    onAnchorClick: (BookDocumentSection<EntryChapter>, String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    onTransitionRetry: (EntryChapter) -> Unit,
) {
    when (item) {
        is BookDocumentViewerItem.Block -> CompositionLocalProvider(
            LocalBookDocumentSectionKey provides item.section.key,
        ) {
            BookDocumentBlockRenderer(
                block = item.content,
                owningContent = item.section.document.document.content,
                sectionKey = item.section.key,
                resourceLoader = item.section.resourceLoader,
                onAnchorClick = { fragment -> onAnchorClick(item.section, fragment) },
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
                preserveTerminalSpacing = item.content.id != item.section.document.blocks.last().id,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
        is BookDocumentViewerItem.Transition -> BookDocumentChapterTransition(
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
