package eu.kanade.tachiyomi.ui.entry.chapters

import android.content.Context
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.state.EntryBookmarkFeature
import mihon.entry.interactions.state.EntryConsumptionFeature
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingProgressInspection
import mihon.entry.interactions.tracking.EntryTrackingProgressSynchronizationResult
import mihon.entry.interactions.tracking.EntryTrackingRefreshResult
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * Marking an entry screen's chapters read or bookmarked, each on the member it belongs to, and the tracker update that
 * marking chapters read offers.
 */
@AssistedInject
class EntryChapterMarks(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val snackbarHostState: SnackbarHostState,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val selection: EntryChapterSelection,
    private val entryRepository: EntryRepository,
    private val entryConsumptionFeature: EntryConsumptionFeature,
    private val entryBookmarkFeature: EntryBookmarkFeature,
    private val entryTrackingFeature: EntryTrackingFeature,
    trackPreferences: TrackPreferences,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            context: Context,
            snackbarHostState: SnackbarHostState,
            state: EntryStateAccess,
            selection: EntryChapterSelection,
        ): EntryChapterMarks
    }

    private val autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead.get()

    fun markPreviousRead(pointer: EntryChapter) {
        val loaded = state.loaded ?: return
        val prevChapters = loaded.readingChapters.map { it.chapter }
        val pointerPos = prevChapters.indexOf(pointer)
        if (pointerPos != -1) markRead(prevChapters.take(pointerPos), true)
    }

    fun markRead(chapters: List<EntryChapter>, read: Boolean) {
        selection.setAll(false)
        if (chapters.isEmpty()) return
        scope.launchIO {
            chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
                val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                entryConsumptionFeature.setConsumed(entry, memberChapters, read)
            }

            if (!read || state.loaded?.hasLoggedInTrackers == false || autoTrackState == AutoTrackState.NEVER) {
                return@launchIO
            }

            refreshTrackers()

            val maxChapterNumber = chapters.maxOf { it.chapterNumber }
            if (
                entryTrackingFeature.inspectProgressSynchronization(
                    state.loaded?.entry ?: return@launchIO,
                    maxChapterNumber,
                ) !is
                    EntryTrackingProgressInspection.UpdateRequired
            ) {
                return@launchIO
            }
            if (autoTrackState == AutoTrackState.ALWAYS) {
                synchronizeTrackingProgress(maxChapterNumber)
                snackbarHostState.showSnackbar(
                    context.stringResource(MR.strings.trackers_updated_summary, maxChapterNumber.toInt()),
                )
                return@launchIO
            }

            val result = snackbarHostState.showSnackbar(
                message = context.stringResource(MR.strings.confirm_tracker_update, maxChapterNumber.toInt()),
                actionLabel = context.stringResource(MR.strings.action_ok),
                duration = SnackbarDuration.Short,
                withDismissAction = true,
            )

            if (result == SnackbarResult.ActionPerformed) {
                synchronizeTrackingProgress(maxChapterNumber)
            }
        }
    }

    fun bookmark(chapters: List<EntryChapter>, bookmarked: Boolean) {
        scope.launchIO {
            chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
                val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                entryBookmarkFeature.setBookmarked(entry, memberChapters, bookmarked)
            }
        }
        selection.setAll(false)
    }

    private suspend fun refreshTrackers() {
        val entry = state.loaded?.entry ?: return
        val result = entryTrackingFeature.refresh(entry)
        (result as? EntryTrackingRefreshResult.Completed)?.failures.orEmpty()
            .forEach { failure ->
                logcat(LogPriority.ERROR, failure.cause) {
                    "Failed to refresh track data entryId=${entry.id} for service ${failure.serviceId.value}"
                }
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.stringResource(
                            MR.strings.track_error,
                            failure.serviceName,
                            failure.cause.message ?: "",
                        ),
                    )
                }
            }
    }

    private suspend fun synchronizeTrackingProgress(progress: Double) {
        val entry = state.loaded?.entry ?: return
        val result = entryTrackingFeature.synchronizeProgress(entry, progress)
        if (result is EntryTrackingProgressSynchronizationResult.Completed) {
            result.failures.forEach { failure -> logcat(LogPriority.WARN, failure.cause) }
        }
    }
}
