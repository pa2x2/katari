package mihon.entry.interactions.history

import mihon.entry.interactions.media.session.EntryMediaSessionActivity
import mihon.entry.interactions.media.session.EntryMediaSessionEvent

/** Feature-owned boundary for recording observed media-session activity. */
interface EntryHistoryFeature {
    suspend fun record(event: EntryMediaSessionEvent, activity: EntryMediaSessionActivity)
}
