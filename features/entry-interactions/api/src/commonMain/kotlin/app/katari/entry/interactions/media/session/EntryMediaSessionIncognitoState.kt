package app.katari.entry.interactions.media.session

/** App-owned source policy; media sessions on an incognito source record no progress or history. */
fun interface EntryMediaSessionIncognitoState {
    fun isIncognito(sourceId: Long?): Boolean
}
