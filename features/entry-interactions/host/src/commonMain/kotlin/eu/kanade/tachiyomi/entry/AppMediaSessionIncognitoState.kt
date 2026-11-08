package eu.kanade.tachiyomi.entry

import app.katari.entry.interactions.media.session.EntryMediaSessionIncognitoState
import eu.kanade.domain.source.interactor.GetIncognitoState

class AppMediaSessionIncognitoState(
    private val getIncognitoState: GetIncognitoState,
) : EntryMediaSessionIncognitoState {
    override fun isIncognito(sourceId: Long?): Boolean {
        return getIncognitoState.await(sourceId)
    }
}
