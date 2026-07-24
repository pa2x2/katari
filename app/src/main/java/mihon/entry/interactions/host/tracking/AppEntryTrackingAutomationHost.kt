package mihon.entry.interactions.host.tracking

import android.app.Application
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.SyncChapterProgressWithTrack
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.model.EntryTrack

internal class AppEntryTrackingAutomationHost(
    private val app: Application,
    private val trackerManager: TrackerManager,
    private val sourceManager: SourceManager,
    private val addTracks: AddTracks,
    private val trackChapter: TrackChapter,
    private val syncChapterProgress: () -> SyncChapterProgressWithTrack,
    private val preferences: TrackPreferences,
) : EntryTrackingAutomationHost {

    override fun isAutomaticProgressSynchronizationEnabled(): Boolean = preferences.autoUpdateTrack.get()

    override suspend fun bindAutomatically(
        entry: Entry,
        serviceIds: Set<Long>,
    ): List<EntryTrackingHostBindingOutcome> {
        return serviceIds.mapNotNull { serviceId ->
            val tracker = trackerManager.get(serviceId) ?: return@mapNotNull null
            try {
                val track = addTracks.bindEnhancedTracker(entry, tracker)
                if (track == null) {
                    EntryTrackingHostBindingOutcome.NoMatch(tracker.id, tracker.name)
                } else {
                    EntryTrackingHostBindingOutcome.Bound(tracker.id, tracker.name, track)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                EntryTrackingHostBindingOutcome.Failed(tracker.id, tracker.name, error)
            }
        }
    }

    override suspend fun synchronizeProgress(
        entryId: Long,
        serviceIds: Set<Long>,
        progress: Double,
        scheduleRetry: Boolean,
    ): List<EntryTrackingHostServiceFailure> {
        return trackChapter.await(
            context = app,
            entryId = entryId,
            chapterNumber = progress,
            serviceIds = serviceIds,
            setupJobOnFailure = scheduleRetry,
        ).map { failure ->
            EntryTrackingHostServiceFailure(failure.serviceId, failure.serviceName, failure.cause)
        }
    }

    override suspend fun reconcileRemoteProgress(
        entry: Entry,
        serviceId: Long,
        track: EntryTrack,
    ) {
        val tracker = trackerManager.get(serviceId) ?: return
        syncChapterProgress().await(entry, track, tracker)
    }

    override suspend fun prepareMigrationTracks(
        source: Entry,
        target: Entry,
        tracks: List<EntryTrack>,
    ): List<EntryTrack> {
        val sourceTracking = sourceManager.get(source.source)?.let {
            EntryTrackingSource.from(it, sourceManager.getDisplayInfo(source.source))
        }
        val targetTracking = sourceManager.get(target.source)?.let {
            EntryTrackingSource.from(it, sourceManager.getDisplayInfo(target.source))
        }
        val enhancedServices = trackerManager.trackers.filterIsInstance<EnhancedTracker>()
        return tracks.mapNotNull { track ->
            val targetTrack = track.copy(entryId = target.id)
            val service = enhancedServices.firstOrNull {
                it.isTrackFrom(targetTrack, source, sourceTracking)
            }
            if (service != null && targetTracking != null) {
                service.migrateTrack(targetTrack, target, targetTracking)
            } else {
                targetTrack
            }
        }
    }
}
