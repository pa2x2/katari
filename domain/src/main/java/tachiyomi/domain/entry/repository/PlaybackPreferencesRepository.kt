package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.PlaybackPreferences

interface PlaybackPreferencesRepository {

    suspend fun getByEntryId(entryId: Long): PlaybackPreferences?

    fun getByEntryIdAsFlow(entryId: Long): Flow<PlaybackPreferences?>

    suspend fun upsert(preferences: PlaybackPreferences)
}
