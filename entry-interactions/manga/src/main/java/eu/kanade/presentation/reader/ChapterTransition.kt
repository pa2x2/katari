package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.service.calculateChapterGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ChapterTransition(
    transition: EntryChildTransition<ReaderChapter>,
    currChapterDownloaded: Boolean,
    goingToChapterDownloaded: Boolean,
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
    val model = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_previous),
            topChild = destinationItem,
            bottomLabel = stringResource(MR.strings.transition_current),
            bottomChild = currentItem,
            fallbackLabel = stringResource(MR.strings.transition_no_previous),
            missingChildCount = calculateChapterGap(
                current?.chapterNumber ?: -1.0,
                destination?.chapterNumber ?: -1.0,
            ),
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = stringResource(MR.strings.transition_finished),
            topChild = currentItem,
            bottomLabel = stringResource(MR.strings.transition_next),
            bottomChild = destinationItem,
            fallbackLabel = stringResource(MR.strings.transition_no_next),
            missingChildCount = calculateChapterGap(
                destination?.chapterNumber ?: -1.0,
                current?.chapterNumber ?: -1.0,
            ),
        )
    }
    ReaderEntryChildTransition(model)
}
