package tachiyomi.domain.history.repository

import tachiyomi.domain.history.model.activity.HistoryActivityPage
import tachiyomi.domain.history.model.activity.HistoryActivityScope

interface HistoryActivityRepository {
    /** [dayOffset] and [dayLimit] count days with activity, newest first. */
    suspend fun getActivityPage(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
        scope: HistoryActivityScope,
        dayOffset: Long,
        dayLimit: Long,
    ): HistoryActivityPage
}
