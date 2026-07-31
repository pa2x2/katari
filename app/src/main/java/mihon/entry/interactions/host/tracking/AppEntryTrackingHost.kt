package mihon.entry.interactions.host.tracking

import android.app.Application
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.SyncChapterProgressWithTrack
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.DeletableTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mihon.entry.interactions.tracking.host.EntryTrackingAccountHost
import mihon.entry.interactions.tracking.host.EntryTrackingAutomationHost
import mihon.entry.interactions.tracking.host.EntryTrackingBackupHost
import mihon.entry.interactions.tracking.host.EntryTrackingCollectionHost
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.entry.interactions.tracking.host.EntryTrackingHostEntryService
import mihon.entry.interactions.tracking.host.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.tracking.host.EntryTrackingHostService
import mihon.entry.interactions.tracking.host.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.tracking.host.EntryTrackingHostStatus
import mihon.entry.interactions.tracking.host.EntryTrackingOperationHost
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerEntry

class AppEntryTrackingHost(
    private val trackerManager: TrackerManager,
    private val sourceManager: SourceManager,
    private val getTracks: GetTracks,
    handler: DatabaseHandler,
    getTracksPerEntry: GetTracksPerEntry,
    refreshTracks: RefreshTracks,
    deleteTrack: DeleteTrack,
    app: Application,
    addTracks: AddTracks,
    trackChapter: TrackChapter,
    syncChapterProgress: () -> SyncChapterProgressWithTrack,
    trackPreferences: TrackPreferences,
) : EntryTrackingHost {

    override val operations: EntryTrackingOperationHost = AppEntryTrackingOperationHost(
        trackerManager = trackerManager,
        refreshTracks = refreshTracks,
        deleteTrack = deleteTrack,
        getTracks = getTracks,
    )
    override val automation: EntryTrackingAutomationHost = AppEntryTrackingAutomationHost(
        app = app,
        trackerManager = trackerManager,
        sourceManager = sourceManager,
        addTracks = addTracks,
        trackChapter = trackChapter,
        syncChapterProgress = syncChapterProgress,
        preferences = trackPreferences,
    )
    override val accounts: EntryTrackingAccountHost = AppEntryTrackingAccountHost(
        trackerManager = trackerManager,
        sourceManager = sourceManager,
    )
    override val collection: EntryTrackingCollectionHost = AppEntryTrackingCollectionHost(
        trackerManager = trackerManager,
        getTracksPerEntry = getTracksPerEntry,
    )
    override val backup: EntryTrackingBackupHost = AppEntryTrackingBackupHost(handler)

    override fun registeredServices(): List<EntryTrackingHostService> {
        return trackerManager.trackers.map(Tracker::toHostService)
    }

    override fun observeEntry(entry: Entry): Flow<EntryTrackingHostEntrySnapshot> {
        val source = sourceManager.getOrStub(entry.source)
        val trackingSource = EntryTrackingSource.from(source, sourceManager.getDisplayInfo(entry.source))
        return combine(
            getTracks.subscribe(entry.id),
            trackerManager.loggedInTrackersFlow(),
        ) { tracks, loggedInTrackers ->
            val tracksByService = tracks.associateBy { it.trackerId }
            val loggedInServiceIds = loggedInTrackers.mapTo(mutableSetOf(), Tracker::id)
            EntryTrackingHostEntrySnapshot(
                services = trackerManager.trackers.map { tracker ->
                    EntryTrackingHostEntryService(
                        service = tracker.toHostService(),
                        isLoggedIn = tracker.id in loggedInServiceIds,
                        acceptsSource = (tracker as? EnhancedTracker)?.accept(trackingSource) ?: true,
                        track = tracksByService[tracker.id],
                        displayScore = tracksByService[tracker.id]?.let(tracker::displayScore),
                    )
                },
            )
        }
    }
}

internal fun Tracker.toHostService(): EntryTrackingHostService {
    return EntryTrackingHostService(
        id = id,
        name = name,
        logoResource = getLogo(),
        supportedEntryTypes = supportedEntryTypes,
        capabilities = EntryTrackingHostServiceCapabilities(
            statuses = getStatusList().map { status ->
                EntryTrackingHostStatus(status, getStatus(status))
            },
            scores = getScoreList(),
            supportsReadingDates = supportsReadingDates,
            supportsPrivateTracking = supportsPrivateTracking,
            supportsRemoteDeletion = this is DeletableTracker,
            supportsAutomaticBinding = this is EnhancedTracker,
        ),
    )
}
