package eu.kanade.presentation.more.settings.screen.tracking

import eu.kanade.domain.track.service.TrackPreferences
import mihon.entry.interactions.tracking.EntryTrackingFeature

/** Graph entry points of the tracking settings. */
interface TrackingSettingsGraph {
    val trackPreferences: TrackPreferences
    val entryTrackingFeature: EntryTrackingFeature
}
