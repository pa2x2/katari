package mihon.entry.interactions

/** Feature-owned boundary for recording observed media-session activity. */
interface EntryHistoryFeature {
    suspend fun record(event: EntryMediaSessionEvent, activity: EntryMediaSessionActivity)
}
