package tachiyomi.domain.history.repository

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.history.model.activity.HistoryActivityPage

interface HistoryActivityRepository {
    suspend fun getActivityPage(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
        type: EntryType?,
        offset: Long,
        limit: Long,
    ): HistoryActivityPage
}
