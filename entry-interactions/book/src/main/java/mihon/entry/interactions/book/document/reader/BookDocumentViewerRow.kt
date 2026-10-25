package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.box.bookDocumentBoxes
import mihon.entry.interactions.book.document.reader.box.textBackdrop
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentTextBackdrop
import mihon.entry.interactions.book.document.reader.transition.BookDocumentChapterTransition
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.presentation.kit.content.ReadingMetrics
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.EntryChapter

/** Renders one stable-key row without invalidating unchanged prose when the chapter window moves. */
@Composable
internal fun BookDocumentViewerRow(
    item: BookDocumentViewerItem<EntryChapter>,
    transitionDirection: EntryChildDirection?,
    loadState: BookDocumentChapterLoadState?,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onTransitionRetry: (EntryChapter) -> Unit,
    transitionVerticalPadding: Dp = ReadingMetrics.chapterBoundaryGap,
    preserveTerminalSpacing: Boolean = true,
    continuesFromPreviousPage: Boolean = false,
    continuesOnNextPage: Boolean = false,
) {
    when (item) {
        is BookDocumentViewerItem.Block -> {
            val selection = LocalBookDocumentChapterSelection.current
            if (selection?.allowsSelection(item.section.owner.id) != false) {
                BookDocumentViewerBlock(
                    item = item,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    preserveTerminalSpacing = preserveTerminalSpacing,
                    continuesFromPreviousPage = continuesFromPreviousPage,
                    continuesOnNextPage = continuesOnNextPage,
                )
            } else {
                DisableSelection {
                    BookDocumentViewerBlock(
                        item = item,
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        preserveTerminalSpacing = preserveTerminalSpacing,
                        continuesFromPreviousPage = continuesFromPreviousPage,
                        continuesOnNextPage = continuesOnNextPage,
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
                    .padding(horizontal = KatariTheme.spacing.xl, vertical = transitionVerticalPadding),
            )
        }
    }
}

@Composable
internal fun BookDocumentViewerBlock(
    item: BookDocumentViewerItem.Block<EntryChapter>,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    preserveTerminalSpacing: Boolean = true,
    continuesFromPreviousPage: Boolean = false,
    continuesOnNextPage: Boolean = false,
) {
    val styling = LocalBookDocumentAuthoredStyling.current
    val textScale = LocalBookDocumentTextScale.current
    val slices =
        remember(item.section, item.content.id, styling.enabled, continuesFromPreviousPage, continuesOnNextPage) {
            if (styling.enabled) {
                item.section.boxIndex.slices(
                    blockIndex = item.section.viewerBlockIndex(item.content.id),
                    startsBlock = !continuesFromPreviousPage,
                    endsBlock = !continuesOnNextPage,
                )
            } else {
                emptyList()
            }
        }
    CompositionLocalProvider(
        LocalBookDocumentSectionKey provides item.section.key,
        LocalBookDocumentResourceLoader provides item.section.resourceLoader,
        LocalBookDocumentSelectionChapterId provides item.section.owner.id,
        LocalBookDocumentTextBackdrop provides slices.textBackdrop(styling),
    ) {
        BookDocumentBlockRenderer(
            block = item.content,
            owningContent = item.section.document.document.content,
            sectionKey = item.section.key,
            resourceLoader = item.section.resourceLoader,
            onAnchorClick = { target -> onAnchorClick(item.section, target) },
            onExternalLinkClick = onExternalLinkClick,
            preserveTerminalSpacing =
            preserveTerminalSpacing && item.content.id != item.section.document.blocks.last().id,
            // Boxes sit inside the page inset and around the gap, so a box's slices meet without a seam.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ReadingMetrics.blockInset)
                .bookDocumentBoxes(slices, styling, BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale)
                .padding(vertical = ReadingMetrics.blockGap),
        )
    }
}
