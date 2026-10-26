package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.box.bookDocumentBoxes
import mihon.entry.interactions.book.document.reader.box.textBackdrop
import mihon.entry.interactions.book.document.reader.floats.BookDocumentFloatWrap
import mihon.entry.interactions.book.document.reader.floats.bookDocumentFloatSlot
import mihon.entry.interactions.book.document.reader.floats.splitAtFloat
import mihon.entry.interactions.book.document.reader.floats.wrapsAroundFloat
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
    wrapFloats: Boolean = false,
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
                    wrapFloats = wrapFloats,
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
                        wrapFloats = wrapFloats,
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

/**
 * [wrapFloats] lets text wrap around a floated block before it. Only the scrolling reader does: a page break would
 * have to fall inside the float's height, and pages measure blocks one at a time, so paged modes stack floats.
 */
@Composable
internal fun BookDocumentViewerBlock(
    item: BookDocumentViewerItem.Block<EntryChapter>,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    preserveTerminalSpacing: Boolean = true,
    continuesFromPreviousPage: Boolean = false,
    continuesOnNextPage: Boolean = false,
    wrapFloats: Boolean = false,
) {
    val styling = LocalBookDocumentAuthoredStyling.current
    val typography = LocalBookDocumentTypography.current
    val textScale = typography.textScale
    val blocks = item.section.document.blocks
    val blockIndex = item.section.viewerBlockIndex(item.content.id)
    val wrappedFloat = blocks.getOrNull(blockIndex - 1)
        ?.takeIf { wrapFloats && it.style.flow.float != null && item.content.wrapsAroundFloat() }
    // A float is drawn by the text it wraps into.
    if (wrapFloats && item.content.style.flow.float != null &&
        blocks.getOrNull(blockIndex + 1)?.wrapsAroundFloat() == true
    ) {
        return
    }
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
        val keepTerminalSpacing = preserveTerminalSpacing && item.content.id != blocks.last().id
        // The gap after a paragraph that closes a box belongs below the box, not inside its border.
        val spacingOutsideBox = if (keepTerminalSpacing && slices.any { it.closes }) {
            bookDocumentTerminalSpacing(
                item.section.document.document.content.text
                    .substring(item.content.logicalStart, item.content.logicalEndExclusive),
                textScale,
            )
        } else {
            0.dp
        }
        val emDp = BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale
        // A float that does not wrap stacks above the text in its own slot, inside the boxes it shares.
        val stackedFloat = item.content.style.flow.float
        val (outerSlices, floatSlices) = if (stackedFloat != null) {
            slices.splitAtFloat(item.section.boxIndex.ownBoxCount(blockIndex))
        } else {
            slices to emptyList()
        }
        // Boxes sit inside the page inset and around the gap, so a box's slices meet without a seam.
        val modifier = Modifier
            .fillMaxWidth()
            .padding(start = typography.blockInset, end = typography.blockInset, bottom = spacingOutsideBox)
            .bookDocumentBoxes(
                slices = outerSlices,
                styling = styling,
                emDp = emDp,
                shrinkable = wrappedFloat == null && stackedFloat == null &&
                    item.content.content is BookDocumentBlockContent.Text,
            )
            .then(
                if (stackedFloat != null) {
                    Modifier.bookDocumentFloatSlot(stackedFloat, emDp).bookDocumentBoxes(floatSlices, styling, emDp)
                } else {
                    Modifier
                },
            )
            .padding(vertical = ReadingMetrics.blockGap)
        val innerTerminalSpacing = keepTerminalSpacing && spacingOutsideBox == 0.dp
        if (wrappedFloat != null) {
            BookDocumentFloatWrap(
                float = wrappedFloat,
                // Only boxes of the float alone go with it; boxes it shares with the text are drawn by the row.
                floatSlices = if (styling.enabled) {
                    item.section.boxIndex.slices(blockIndex - 1)
                        .splitAtFloat(item.section.boxIndex.ownBoxCount(blockIndex - 1)).second
                } else {
                    emptyList()
                },
                text = item.content,
                owningContent = item.section.document.document.content,
                sectionKey = item.section.key,
                resourceLoader = item.section.resourceLoader,
                onAnchorClick = { target -> onAnchorClick(item.section, target) },
                onExternalLinkClick = onExternalLinkClick,
                preserveTerminalSpacing = innerTerminalSpacing,
                modifier = modifier,
            )
        } else {
            BookDocumentBlockRenderer(
                block = item.content,
                owningContent = item.section.document.document.content,
                sectionKey = item.section.key,
                resourceLoader = item.section.resourceLoader,
                onAnchorClick = { target -> onAnchorClick(item.section, target) },
                onExternalLinkClick = onExternalLinkClick,
                preserveTerminalSpacing = innerTerminalSpacing,
                modifier = modifier,
            )
        }
    }
}
