package eu.kanade.tachiyomi.data.track.oauth

import eu.kanade.tachiyomi.data.track.TrackerManager

/** Graph entry points of the tracker login activities, which Android constructs. */
interface TrackLoginGraph {
    val trackerManager: TrackerManager
}
