package tachiyomi.domain.statistics.recap

import kotlinx.coroutines.flow.Flow

/** Activity for recaps; local dates are inclusive `yyyy-MM-dd` bounds. */
interface StatisticsRecapRepository {
    suspend fun getActivity(profileId: Long, startLocalDate: String, endLocalDate: String): StatisticsRecapActivity

    suspend fun getPeriodTotals(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
    ): StatisticsRecapPeriodTotals

    fun subscribeHasActivity(profileId: Long, startLocalDate: String, endLocalDate: String): Flow<Boolean>

    /** Months with timed activity as `yyyy-MM`, latest first. */
    fun subscribeActiveMonths(profileId: Long): Flow<List<String>>
}
