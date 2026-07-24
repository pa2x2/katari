package mihon.entry.interactions

/** Consequence families that independently contributed media-session policy may suppress. */
enum class EntryMediaSessionConsequence {
    RECORD_PROGRESS,
    RECORD_HISTORY,
    SYNCHRONIZE_TRACKING,
}

/** App-owned source policy used by the incognito media-session participant. */
fun interface EntryMediaSessionIncognitoState {
    fun isIncognito(sourceId: Long?): Boolean
}
