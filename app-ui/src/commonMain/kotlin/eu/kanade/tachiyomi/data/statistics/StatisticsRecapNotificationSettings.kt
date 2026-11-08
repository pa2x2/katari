package eu.kanade.tachiyomi.data.statistics

import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.stats.data.StatsRecapNotifications
import eu.kanade.tachiyomi.ui.stats.recap.delivery.editionKey
import eu.kanade.tachiyomi.ui.stats.recap.delivery.newYearRecap
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule
import java.time.LocalDate
import java.time.YearMonth

@Inject
class StatisticsRecapNotificationSettings(
    private val profileStore: ProfileStore,
    private val recapSchedule: StatisticsRecapSchedule,
) {
    /**
     * Turning a recap on starts with the next one due, so a recap never arrives right away for a period that's
     * already over.
     */
    suspend fun setEnabled(profileId: Long, notifications: StatsRecapNotifications) {
        val preferences = StatisticsPreferences(profileStore.profileStore(profileId))
        if (notifications.monthly && !preferences.monthlyRecapNotification.get()) {
            preferences.lastRecapNotificationMonth.set(YearMonth.now().minusMonths(1L).toString())
        }
        if (notifications.yearly && !preferences.yearlyRecapNotification.get()) {
            newYearRecap(LocalDate.now())?.let { preferences.lastYearRecapNotification.set(it.editionKey) }
        }
        preferences.monthlyRecapNotification.set(notifications.monthly)
        preferences.yearlyRecapNotification.set(notifications.yearly)
        recapSchedule.setupTask()
    }
}
