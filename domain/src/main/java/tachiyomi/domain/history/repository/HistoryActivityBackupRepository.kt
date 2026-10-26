package tachiyomi.domain.history.repository

import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot

interface HistoryActivityBackupRepository {

    /** The activity of each of [entryIds] that has any; the others are left out. */
    suspend fun getActivityByEntryIds(entryIds: Collection<Long>): Map<Long, HistoryActivitySnapshot>

    suspend fun restoreActivity(entryId: Long, snapshot: HistoryActivitySnapshot)

    suspend fun getStatisticsEpoch(profileId: Long): Long?

    suspend fun restoreStatisticsEpoch(profileId: Long, startedAtEpochMillis: Long)
}
