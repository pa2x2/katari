package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionDestinationSlot
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadState
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.i18n.stringResource

/** BOOK metadata adapter for the shared entry-child transition surface. */
@Composable
internal fun BookDocumentChapterTransition(
    transition: EntryChildTransition<EntryChapter>,
    loadState: BookDocumentChapterLoadState?,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val current = transition.from.toTransitionItem()
    val destination = transition.to?.toTransitionItem()
    val sharedLoadState = when (loadState) {
        null -> ReaderEntryChildTransitionLoadState.Idle
        BookDocumentChapterLoadState.Loading -> ReaderEntryChildTransitionLoadState.Loading(
            stringResource(MR.strings.loading),
        )
        is BookDocumentChapterLoadState.Failed -> ReaderEntryChildTransitionLoadState.Failed(loadState.message)
    }
    val model = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_previous),
            topChild = destination,
            bottomLabel = stringResource(MR.strings.transition_current),
            bottomChild = current,
            fallbackLabel = stringResource(MR.strings.transition_no_previous),
            destinationLoadState = sharedLoadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.TOP,
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_finished),
            topChild = current,
            bottomLabel = stringResource(MR.strings.transition_next),
            bottomChild = destination,
            fallbackLabel = stringResource(MR.strings.transition_no_next),
            destinationLoadState = sharedLoadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.BOTTOM,
        )
    }
    ReaderEntryChildTransition(model = model, onRetry = onRetry, modifier = modifier)
}

private fun EntryChapter.toTransitionItem() = ReaderEntryChildTransitionItem(
    name = name,
    subtitle = scanlator,
)
