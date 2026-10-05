package tachiyomi.domain.history.repository

import tachiyomi.domain.history.model.activity.HistoryActivityPage
import tachiyomi.domain.history.model.activity.HistoryActivityScope

interface HistoryActivityRepository {
    suspend fun getActivityPage(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
        scope: HistoryActivityScope,
        offset: Long,
        limit: Long,
    ): HistoryActivityPage
}
