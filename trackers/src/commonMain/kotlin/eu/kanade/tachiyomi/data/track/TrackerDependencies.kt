package eu.kanade.tachiyomi.data.track

import dev.zacsweers.metro.Inject
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import mihon.domain.app.AppIdentity
import tachiyomi.domain.track.interactor.InsertTrack

/**
 * Collaborators every [BaseTracker] shares. [TrackerManager] creates all trackers up front, so the heavier services
 * stay lazy until a tracker first uses them.
 */
@Inject
class TrackerDependencies(
    val appIdentity: AppIdentity,
    val failureMessages: TrackerFailureMessages,
    val json: Json,
    val trackPreferences: Lazy<TrackPreferences>,
    val networkHelper: Lazy<NetworkHelper>,
    val addTracks: Lazy<AddTracks>,
    val insertTrack: Lazy<InsertTrack>,
)
