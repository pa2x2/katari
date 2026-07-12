package eu.kanade.tachiyomi.entry

import android.content.Context
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.service.TrackPreferences
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.EntryReaderTracking

class AppReaderIncognitoState(
    private val getIncognitoState: GetIncognitoState,
) : EntryReaderIncognitoState {
    override fun isIncognito(sourceId: Long?): Boolean {
        return getIncognitoState.await(sourceId)
    }
}

class AppReaderTracking(
    private val trackPreferences: TrackPreferences,
    private val trackChapter: TrackChapter,
) : EntryReaderTracking {
    override suspend fun updateChapterRead(context: Context, entryId: Long, chapterNumber: Double) {
        if (!trackPreferences.autoUpdateTrack.get()) return

        trackChapter.await(context, entryId, chapterNumber)
    }
}
