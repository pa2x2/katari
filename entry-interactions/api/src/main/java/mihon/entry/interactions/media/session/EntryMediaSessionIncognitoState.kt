package mihon.entry.interactions.media.session

/** App-owned source policy; media sessions on an incognito source record no progress, history or tracking. */
fun interface EntryMediaSessionIncognitoState {
    fun isIncognito(sourceId: Long?): Boolean
}
