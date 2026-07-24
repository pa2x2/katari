package tachiyomi.data.track

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.domain.track.repository.TrackRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TrackRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : TrackRepository {

    override suspend fun getTrackById(id: Long): EntryTrack? {
        return handler.awaitOneOrNull {
            entry_syncQueries.getTrackById(id, profileProvider.activeProfileId, TrackMapper::mapTrack)
        }
    }

    override suspend fun getTracksByEntryId(entryId: Long): List<EntryTrack> {
        return handler.awaitList {
            entry_syncQueries.getTracksByEntryId(profileProvider.activeProfileId, entryId, TrackMapper::mapTrack)
        }
    }

    override fun getTracksAsFlow(): Flow<List<EntryTrack>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entry_syncQueries.getTracks(profileId, TrackMapper::mapTrack)
            }
        }
    }

    override fun getTracksByEntryIdAsFlow(entryId: Long): Flow<List<EntryTrack>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entry_syncQueries.getTracksByEntryId(profileId, entryId, TrackMapper::mapTrack)
            }
        }
    }

    override suspend fun delete(entryId: Long, trackerId: Long) {
        handler.await {
            entry_syncQueries.delete(
                profileId = profileProvider.activeProfileId,
                entryId = entryId,
                syncId = trackerId,
            )
        }
    }

    override suspend fun insert(track: EntryTrack) {
        insertValues(profileProvider.activeProfileId, track)
    }

    override suspend fun insert(profileId: Long, track: EntryTrack) {
        insertValues(profileId, track)
    }

    override suspend fun insertAll(tracks: List<EntryTrack>) {
        insertValues(profileProvider.activeProfileId, *tracks.toTypedArray())
    }

    override suspend fun insertAll(profileId: Long, tracks: List<EntryTrack>) {
        insertValues(profileId, *tracks.toTypedArray())
    }

    private suspend fun insertValues(profileId: Long, vararg tracks: EntryTrack) {
        handler.await(inTransaction = true) {
            tracks.forEach { track ->
                entry_syncQueries.insert(
                    profileId = profileId,
                    entryId = track.entryId,
                    syncId = track.trackerId,
                    remoteId = track.remoteId,
                    libraryId = track.libraryId,
                    title = track.title,
                    lastChapterRead = track.progress,
                    totalChapters = track.total,
                    status = track.status,
                    score = track.score,
                    remoteUrl = track.remoteUrl,
                    startDate = track.startDate,
                    finishDate = track.finishDate,
                    private = track.private,
                )
            }
        }
    }
}
