package tachiyomi.domain.track.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.model.EntryTrack

interface TrackRepository {

    suspend fun getTrackById(id: Long): EntryTrack?

    suspend fun getTracksByEntryId(entryId: Long): List<EntryTrack>

    fun getTracksAsFlow(): Flow<List<EntryTrack>>

    fun getTracksByEntryIdAsFlow(entryId: Long): Flow<List<EntryTrack>>

    suspend fun delete(entryId: Long, trackerId: Long)

    suspend fun insert(track: EntryTrack)

    suspend fun insertAll(tracks: List<EntryTrack>)
}
