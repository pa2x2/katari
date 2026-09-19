package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.service.calculateChapterGap
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionDestinationSlot
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadState
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadingIndicator
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Layout context the transition is rendered in; the surrounding holders no longer own the
 * breathing room so the padding can follow the display mode while the view stays on screen.
 */
internal enum class ChapterTransitionPlacement {
    PAGER,
    WEBTOON,
}

/**
 * True when the transition must render as a compact loading indicator instead of the chapter card.
 *
 * Hidden mode suppresses the card only while the destination chapter is being loaded into a
 * contiguous window; a negative gap from duplicate numbering also counts as contiguous, matching
 * the adapters' missing-chapter checks. Everything that cannot be resolved by loading - chapter
 * gaps, end of content and load failures - keeps the full card so the reader still explains what
 * happened instead of spinning forever. A rendered transition item's destination is never already
 * loaded: the adapters drop the item the moment the neighbor chapter becomes loaded.
 */
internal fun rendersCompactTransitionLoading(
    displayMode: ChapterTransitionMode,
    hasDestination: Boolean,
    destinationLoadState: ReaderEntryChildTransitionLoadState,
    chapterGap: Int,
): Boolean = displayMode == ChapterTransitionMode.HIDDEN &&
    hasDestination &&
    chapterGap <= 0 &&
    destinationLoadState !is ReaderEntryChildTransitionLoadState.Failed

@Composable
internal fun ChapterTransition(
    transition: EntryChildTransition<ReaderChapter>,
    currChapterDownloaded: Boolean,
    goingToChapterDownloaded: Boolean,
    loadState: ReaderEntryChildTransitionLoadState,
    onRetry: (() -> Unit)?,
    displayMode: ChapterTransitionMode,
    placement: ChapterTransitionPlacement,
) {
    val current = transition.from.chapter.toDomainChapter()
    val destination = transition.to?.chapter?.toDomainChapter()
    val currentItem = current?.let {
        ReaderEntryChildTransitionItem(
            name = it.name,
            subtitle = it.scanlator,
            availableOffline = currChapterDownloaded,
        )
    }
    val destinationItem = destination?.let {
        ReaderEntryChildTransitionItem(
            name = it.name,
            subtitle = it.scanlator,
            availableOffline = goingToChapterDownloaded,
        )
    }
    val gapCount = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> calculateChapterGap(
            current?.chapterNumber ?: -1.0,
            destination?.chapterNumber ?: -1.0,
        )
        EntryChildDirection.NEXT -> calculateChapterGap(
            destination?.chapterNumber ?: -1.0,
            current?.chapterNumber ?: -1.0,
        )
    }

    if (rendersCompactTransitionLoading(displayMode, transition.to != null, loadState, gapCount)) {
        ChapterTransitionLoadingIndicator(placement = placement)
        return
    }

    val model = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_previous),
            topChild = destinationItem,
            bottomLabel = stringResource(MR.strings.transition_current),
            bottomChild = currentItem,
            fallbackLabel = stringResource(MR.strings.transition_no_previous),
            missingChildCount = gapCount,
            destinationLoadState = loadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.TOP,
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_finished),
            topChild = currentItem,
            bottomLabel = stringResource(MR.strings.transition_next),
            bottomChild = destinationItem,
            fallbackLabel = stringResource(MR.strings.transition_no_next),
            missingChildCount = gapCount,
            destinationLoadState = loadState,
            destinationSlot = ReaderEntryChildTransitionDestinationSlot.BOTTOM,
        )
    }
    ReaderEntryChildTransition(
        model = model,
        onRetry = onRetry,
        modifier = when (placement) {
            ChapterTransitionPlacement.PAGER -> Modifier
            ChapterTransitionPlacement.WEBTOON -> Modifier.padding(horizontal = 32.dp, vertical = 128.dp)
        },
    )
}

/**
 * Minimal progress affordance shown between contiguous chapters while the destination loads.
 * It occupies the slot the content will occupy, so no further interaction is needed once the
 * chapter finishes loading.
 */
@Composable
private fun ChapterTransitionLoadingIndicator(placement: ChapterTransitionPlacement) {
    val isWebtoon = placement == ChapterTransitionPlacement.WEBTOON
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isWebtoon) 32.dp else 0.dp,
                vertical = if (isWebtoon) 64.dp else 0.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ReaderEntryChildTransitionLoadingIndicator(
            loadingDescription = stringResource(MR.strings.transition_pages_loading),
        )
    }
}
