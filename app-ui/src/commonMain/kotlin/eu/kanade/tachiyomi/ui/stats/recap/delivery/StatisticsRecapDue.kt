package eu.kanade.tachiyomi.ui.stats.recap.delivery

import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.service.StatisticsPreferences
import java.time.LocalDate
import java.time.YearMonth

/**
 * The recaps of a profile that are due to be told about, each once: the previous month's once it is over, and the
 * year's on December 1 (so far) and January 1 (whole year), when the profile asks for them.
 *
 * Checked daily rather than on those dates, so a device that is off or idle that day still gets the recap later.
 */
@Inject
class StatisticsRecapDue(
    private val profileStore: ProfileStore,
    private val repository: StatisticsRecapRepository,
) {
    /**
     * Hands each recap of [profileId] that is due to [tell], then records it as told. A period with nothing timed is
     * recorded without being handed over, so an empty month gets no notification.
     */
    suspend fun deliver(profileId: Long, tell: (DueStatisticsRecap) -> Unit) {
        val preferences = StatisticsPreferences(profileStore.profileStore(profileId))
        deliverMonth(profileId, preferences, tell)
        deliverYear(profileId, preferences, tell)
    }

    private suspend fun deliverMonth(
        profileId: Long,
        preferences: StatisticsPreferences,
        tell: (DueStatisticsRecap) -> Unit,
    ) {
        if (!preferences.monthlyRecapNotification.get()) return
        val month = YearMonth.now().minusMonths(1L)
        if (preferences.lastRecapNotificationMonth.get() >= month.toString()) return
        val period = StatisticsRecapPeriod.Month(month)
        headlineOf(profileId, period)?.let { tell(DueStatisticsRecap.Month(profileId, period, it)) }
        preferences.lastRecapNotificationMonth.set(month.toString())
    }

    private suspend fun deliverYear(
        profileId: Long,
        preferences: StatisticsPreferences,
        tell: (DueStatisticsRecap) -> Unit,
    ) {
        if (!preferences.yearlyRecapNotification.get()) return
        val period = newYearRecap(LocalDate.now()) ?: return
        if (preferences.lastYearRecapNotification.get() == period.editionKey) return
        headlineOf(profileId, period)?.let { tell(DueStatisticsRecap.Year(profileId, period, it)) }
        preferences.lastYearRecapNotification.set(period.editionKey)
    }

    private suspend fun headlineOf(profileId: Long, period: StatisticsRecapPeriod): StatisticsRecapHeadline? {
        val activity = repository.getActivity(profileId, period.start.toString(), period.end.toString())
        return recapHeadline(period, activity)
    }
}

/** A recap of [profileId] to tell about. */
sealed interface DueStatisticsRecap {
    val profileId: Long
    val period: StatisticsRecapPeriod
    val headline: StatisticsRecapHeadline

    data class Month(
        override val profileId: Long,
        override val period: StatisticsRecapPeriod.Month,
        override val headline: StatisticsRecapHeadline,
    ) : DueStatisticsRecap

    data class Year(
        override val profileId: Long,
        override val period: StatisticsRecapPeriod.Year,
        override val headline: StatisticsRecapHeadline,
    ) : DueStatisticsRecap
}
