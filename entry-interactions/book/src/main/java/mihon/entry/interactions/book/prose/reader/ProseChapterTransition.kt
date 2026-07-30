package mihon.entry.interactions.book.prose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.calculateChapterGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionDestinationSlot
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadState
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.i18n.stringResource as i18nStringResource

@Composable
internal fun ProseTransition(
    transition: EntryChildTransition<EntryChapter>,
    loadState: ReaderEntryChildTransitionLoadState,
    onRetry: (() -> Unit)?,
    palette: ProsePalette,
    modifier: Modifier = Modifier,
) {
    val current = transition.from.toTransitionItem()
    val destination = transition.to?.toTransitionItem()
    val model = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = i18nStringResource(MR.strings.transition_previous),
            topChild = destination,
            bottomLabel = i18nStringResource(MR.strings.transition_current),
            bottomChild = current,
            fallbackLabel = i18nStringResource(MR.strings.transition_no_previous),
            missingChildCount = calculateChapterGap(
                transition.from.chapterNumber,
                transition.to?.chapterNumber ?: -1.0,
            ),
            destinationLoadState = loadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.TOP,
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = i18nStringResource(MR.strings.transition_finished),
            topChild = current,
            bottomLabel = i18nStringResource(MR.strings.transition_next),
            bottomChild = destination,
            fallbackLabel = i18nStringResource(MR.strings.transition_no_next),
            missingChildCount = calculateChapterGap(
                transition.to?.chapterNumber ?: -1.0,
                transition.from.chapterNumber,
            ),
            destinationLoadState = loadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.BOTTOM,
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ReaderEntryChildTransition(
            model = model,
            onRetry = onRetry,
            contentColor = palette.foreground,
            accentColor = palette.foreground,
            warningColor = palette.foreground,
            outlineColor = palette.foreground.copy(alpha = 0.38f),
        )
    }
}

@Composable
internal fun HtmlProseChapterLoadState?.toSharedLoadState(): ReaderEntryChildTransitionLoadState =
    when (this) {
        null -> ReaderEntryChildTransitionLoadState.Idle
        HtmlProseChapterLoadState.Loading -> ReaderEntryChildTransitionLoadState.Loading(
            i18nStringResource(MR.strings.loading),
        )
        is HtmlProseChapterLoadState.Failed -> ReaderEntryChildTransitionLoadState.Failed(message)
    }
