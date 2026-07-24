package eu.kanade.tachiyomi.entry

import eu.kanade.domain.source.interactor.GetIncognitoState
import mihon.entry.interactions.EntryMediaSessionIncognitoState

class AppMediaSessionIncognitoState(
    private val getIncognitoState: GetIncognitoState,
) : EntryMediaSessionIncognitoState {
    override fun isIncognito(sourceId: Long?): Boolean {
        return getIncognitoState.await(sourceId)
    }
}
