package tachiyomi.domain.history.repository

import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot

interface HistoryActivityBackupRepository {

    suspend fun getActivityByEntryId(entryId: Long): HistoryActivitySnapshot

    suspend fun restoreActivity(entryId: Long, snapshot: HistoryActivitySnapshot)

    suspend fun getStatisticsEpoch(profileId: Long): Long?

    suspend fun restoreStatisticsEpoch(profileId: Long, startedAtEpochMillis: Long)
}
