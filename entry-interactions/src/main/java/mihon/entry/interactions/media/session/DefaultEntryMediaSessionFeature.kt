package mihon.entry.interactions.media.session

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadLifecycleEvent
import mihon.entry.interactions.download.EntryDownloadLifecycleFeature
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.state.EntryProgressFeature
import mihon.entry.interactions.state.EntryProgressRecordingResult
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.translate.ahead.EntryTranslateAhead
import tachiyomi.domain.entry.repository.EntryRepository

/**
 * Applies the shared consequences of a media runtime's event, in order. Progress is recorded first because downloads,
 * history and tracking react to a child completing, and translate ahead runs after downloads have queued what they
 * fetch. An incognito source records no progress, history or tracking.
 */
internal class DefaultEntryMediaSessionFeature(
    private val types: EntryMediaSessionTypes,
    private val incognitoState: EntryMediaSessionIncognitoState,
    private val entryRepository: () -> EntryRepository,
    private val progress: () -> EntryProgressFeature,
    private val downloads: () -> EntryDownloadLifecycleFeature,
    private val history: () -> EntryHistoryFeature,
    private val tracking: () -> EntryTrackingFeature,
    private val automaticTrackingSynchronizationEnabled: () -> Boolean,
    private val translateAhead: () -> EntryTranslateAhead,
) : EntryMediaSessionFeature {
    override fun isApplicable(type: EntryType): Boolean = type in types.mediaSession

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        val type = event.visibleEntry.type
        if (!isApplicable(type)) return EntryMediaSessionResult.Inapplicable(type)

        val recording = !isIncognito(event)
        val progressed = event as? EntryMediaSessionEvent.Progressed
        val recorded = progressed
            ?.takeIf { recording && type in types.progress }
            ?.let { progress().recordMediaProgress(it) }
        if (progressed != null && type in types.download) updateDownloads(progressed, recorded)
        if (recording) recordHistory(event, recorded)
        if (progressed != null && recording && type in types.progress) synchronizeTracking(progressed, recorded)
        if (progressed != null && type in types.translateAhead) translateAhead(progressed)
        return EntryMediaSessionResult.Handled
    }

    private suspend fun isIncognito(event: EntryMediaSessionEvent): Boolean {
        val owner = entryRepository().getEntryById(event.child.entryId)
        return incognitoState.isIncognito(owner?.source ?: event.visibleEntry.source)
    }

    private suspend fun updateDownloads(
        event: EntryMediaSessionEvent.Progressed,
        recorded: EntryProgressRecordingResult?,
    ) {
        event.fraction?.let { fraction ->
            downloads().onEvent(
                EntryDownloadLifecycleEvent.Progressed(
                    visibleEntry = event.visibleEntry,
                    child = event.child,
                    fraction = fraction,
                    deduplicateByNumber = event.deduplicateDownloadByNumber,
                ),
            )
        }
        if (recorded?.completedNow == true) {
            downloads().onEvent(
                EntryDownloadLifecycleEvent.Completed(
                    visibleEntry = event.visibleEntry,
                    child = event.child,
                    deduplicateByNumber = event.deduplicateDownloadByNumber,
                    completeEquivalentChildrenByNumber = event.completeEquivalentChildrenByNumber,
                ),
            )
        }
    }

    private suspend fun recordHistory(event: EntryMediaSessionEvent, recorded: EntryProgressRecordingResult?) {
        when (event) {
            is EntryMediaSessionEvent.Progressed -> {
                event.activity?.let { history().record(event, it) }
                recorded?.takeIf { it.completedNow }?.let { history().recordCompletion(event, it.state) }
            }
            is EntryMediaSessionEvent.ActivityRecorded -> history().record(event, event.activity)
        }
    }

    private suspend fun synchronizeTracking(
        event: EntryMediaSessionEvent.Progressed,
        recorded: EntryProgressRecordingResult?,
    ) {
        if (recorded?.completedNow != true) return
        if (event.child.chapterNumber < 0.0) return
        if (!automaticTrackingSynchronizationEnabled()) return
        tracking().synchronizeProgress(entry = event.visibleEntry, progress = event.child.chapterNumber)
    }

    private suspend fun translateAhead(event: EntryMediaSessionEvent.Progressed) {
        val fraction = event.fraction ?: return
        translateAhead().onProgressed(event.visibleEntry, event.child, fraction, event.deduplicateDownloadByNumber)
    }
}

/** The types each consequence applies to, read from the providers the type modules contribute. */
internal class EntryMediaSessionTypes(
    val mediaSession: Set<EntryType>,
    val progress: Set<EntryType>,
    val download: Set<EntryType>,
    val translateAhead: Set<EntryType>,
)
