package tachiyomi.data.entry

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntrySync
import tachiyomi.domain.entry.repository.EntrySyncRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EntrySyncRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntrySyncRepository {

    override suspend fun getTrackById(id: Long): EntrySync? {
        return handler.awaitOneOrNull {
            entry_syncQueries.getTrackById(id, profileProvider.activeProfileId, EntrySyncMapper::mapTrack)
        }
    }

    override suspend fun getTracksByEntryId(entryId: Long): List<EntrySync> {
        return handler.awaitList {
            entry_syncQueries.getTracksByEntryId(profileProvider.activeProfileId, entryId, EntrySyncMapper::mapTrack)
        }
    }

    override fun getTracksAsFlow(): Flow<List<EntrySync>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entry_syncQueries.getTracks(profileId, EntrySyncMapper::mapTrack)
            }
        }
    }

    override fun getTracksByEntryIdAsFlow(entryId: Long): Flow<List<EntrySync>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entry_syncQueries.getTracksByEntryId(profileId, entryId, EntrySyncMapper::mapTrack)
            }
        }
    }

    override suspend fun delete(entryId: Long, syncId: Long) {
        handler.await {
            entry_syncQueries.delete(
                profileId = profileProvider.activeProfileId,
                entryId = entryId,
                syncId = syncId,
            )
        }
    }

    override suspend fun insert(sync: EntrySync) {
        insertValues(sync)
    }

    override suspend fun insertAll(syncs: List<EntrySync>) {
        insertValues(*syncs.toTypedArray())
    }

    private suspend fun insertValues(vararg syncs: EntrySync) {
        handler.await(inTransaction = true) {
            syncs.forEach { sync ->
                entry_syncQueries.insert(
                    profileId = profileProvider.activeProfileId,
                    entryId = sync.entryId,
                    syncId = sync.syncId,
                    remoteId = sync.remoteId,
                    libraryId = sync.libraryId,
                    title = sync.title,
                    lastChapterRead = sync.progress,
                    totalChapters = sync.total,
                    status = sync.status,
                    score = sync.score,
                    remoteUrl = sync.remoteUrl,
                    startDate = sync.startDate,
                    finishDate = sync.finishDate,
                    private = sync.private,
                )
            }
        }
    }
}
