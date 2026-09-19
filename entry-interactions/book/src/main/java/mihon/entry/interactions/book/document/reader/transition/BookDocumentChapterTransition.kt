package mihon.entry.interactions.book.document.reader.transition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionDestinationSlot
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadState
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadingIndicator
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The chapter-transition display mode shared with the manga reader, provided around the whole
 * book reader surface so every boundary row follows the same setting without threading it
 * through the viewport signatures.
 */
internal val LocalBookDocumentChapterTransitionMode = staticCompositionLocalOf {
    ChapterTransitionMode.ALWAYS
}

/**
 * True when hidden mode renders a compact boundary instead of the card: a populated, contiguous,
 * non-failed boundary shows the compact progress indicator while its destination prepares and pure
 * spacing once prepared. Gapped boundaries, terminal boundaries and load failures keep the full
 * card - carrying the missing-chapter count - so the reader still explains what happened instead
 * of hiding the way forward.
 */
internal fun rendersCompactHiddenBoundary(
    displayMode: ChapterTransitionMode,
    transition: EntryChildTransition<*>,
    destinationLoadState: BookDocumentChapterLoadState?,
    chapterGap: Int,
): Boolean = displayMode == ChapterTransitionMode.HIDDEN &&
    transition.to != null &&
    chapterGap <= 0 &&
    destinationLoadState !is BookDocumentChapterLoadState.Failed

/**
 * BOOK metadata adapter for the shared entry-child transition surface.
 *
 * The boundary row is structural - it always exists between two chapters - so hidden mode must
 * distinguish states the manga adapters never render: a boundary whose destination document is
 * already prepared keeps only its spacing, one that is preparing shows the compact progress
 * indicator, and terminal, gapped and failed boundaries keep the full card so the reader still
 * explains what happened instead of spinning forever.
 */
@Composable
internal fun BookDocumentChapterTransition(
    transition: EntryChildTransition<EntryChapter>,
    direction: EntryChildDirection,
    loadState: BookDocumentChapterLoadState?,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val displayMode = LocalBookDocumentChapterTransitionMode.current
    val isPreparing = loadState is BookDocumentChapterLoadState.Loading
    val chapterGap = boundaryChapterGap(transition)

    if (rendersCompactHiddenBoundary(displayMode, transition, loadState, chapterGap)) {
        if (isPreparing) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                ReaderEntryChildTransitionLoadingIndicator(
                    loadingDescription = stringResource(MR.strings.transition_pages_loading),
                    accentColor = palette.accent,
                )
            }
        } else {
            Spacer(modifier = modifier)
        }
        return
    }

    val current = transition.from.toTransitionItem()
    val destination = transition.to?.toTransitionItem()
    val sharedLoadState = when (loadState) {
        null -> ReaderEntryChildTransitionLoadState.Idle
        BookDocumentChapterLoadState.Loading -> ReaderEntryChildTransitionLoadState.Loading(
            stringResource(MR.strings.loading),
        )
        is BookDocumentChapterLoadState.Failed -> ReaderEntryChildTransitionLoadState.Failed(loadState.message)
    }
    val model = when (direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_previous),
            topChild = destination,
            bottomLabel = stringResource(MR.strings.transition_current),
            bottomChild = current,
            fallbackLabel = stringResource(MR.strings.transition_no_previous),
            missingChildCount = chapterGap,
            destinationLoadState = sharedLoadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.TOP,
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_finished),
            topChild = current,
            bottomLabel = stringResource(MR.strings.transition_next),
            bottomChild = destination,
            fallbackLabel = stringResource(MR.strings.transition_no_next),
            missingChildCount = chapterGap,
            destinationLoadState = sharedLoadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.BOTTOM,
        )
    }
    ReaderEntryChildTransition(
        model = model,
        onRetry = onRetry,
        modifier = modifier,
        contentColor = palette.foreground,
        accentColor = palette.accent,
        warningColor = palette.warning,
        outlineColor = palette.outline,
    )
}

private fun EntryChapter.toTransitionItem() = ReaderEntryChildTransitionItem(
    name = name,
    subtitle = scanlator,
)
