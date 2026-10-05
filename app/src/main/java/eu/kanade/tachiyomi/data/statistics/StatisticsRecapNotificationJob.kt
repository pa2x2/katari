package eu.kanade.tachiyomi.data.statistics

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.presentation.more.stats.components.statisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsRecapNotifications
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapNavigation
import eu.kanade.tachiyomi.ui.stats.recap.delivery.editionKey
import eu.kanade.tachiyomi.ui.stats.recap.delivery.newYearRecap
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapSummary
import eu.kanade.tachiyomi.ui.stats.recap.story.SummaryFigure
import eu.kanade.tachiyomi.ui.stats.recap.story.buildStatisticsRecapStory
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Posts the active profile's recaps: the previous month's once it's over, and the year's on December 1 (so far) and
 * January 1 (whole year).
 *
 * Runs daily rather than on those dates, so a device that is off or idle that day still gets the recap later.
 */
class StatisticsRecapNotificationJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val profileId = Injekt.get<ActiveProfileProvider>().activeProfileId
        val preferences = StatisticsPreferences(Injekt.get<ProfileStore>().profileStore(profileId))
        return try {
            notifyMonthIfDue(profileId, preferences)
            notifyYearIfDue(profileId, preferences)
            Result.success()
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error)
            Result.retry()
        }
    }

    private suspend fun notifyMonthIfDue(profileId: Long, preferences: StatisticsPreferences) {
        if (!preferences.monthlyRecapNotification.get()) return
        val month = YearMonth.now().minusMonths(1L)
        if (preferences.lastRecapNotificationMonth.get() >= month.toString()) return
        val period = StatisticsRecapPeriod.Month(month)
        summaryOf(profileId, period)?.let { summary ->
            val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            val activeDays = summary.figures.filterIsInstance<SummaryFigure.ActiveDays>().firstOrNull()?.days ?: 0
            notify(
                id = Notifications.ID_STATISTICS_RECAP,
                profileId = profileId,
                period = period,
                title = applicationContext.stringResource(
                    MR.strings.statistics_recap_notification_title,
                    monthName,
                    applicationContext.statisticsDurationFormatter()(summary.durationMillis),
                ),
                text = applicationContext.stringResource(
                    MR.strings.statistics_recap_notification_text,
                    applicationContext.pluralStringResource(
                        MR.plurals.statistics_active_day_count,
                        activeDays,
                        activeDays,
                    ),
                ),
            )
        }
        preferences.lastRecapNotificationMonth.set(month.toString())
    }

    private suspend fun notifyYearIfDue(profileId: Long, preferences: StatisticsPreferences) {
        if (!preferences.yearlyRecapNotification.get()) return
        val period = newYearRecap(LocalDate.now()) ?: return
        if (preferences.lastYearRecapNotification.get() == period.editionKey) return
        summaryOf(profileId, period)?.let { summary ->
            val label = if (period.isSoFar) {
                applicationContext.stringResource(MR.strings.statistics_recap_year_so_far, period.year.toString())
            } else {
                period.year.toString()
            }
            notify(
                id = Notifications.ID_STATISTICS_YEAR_RECAP,
                profileId = profileId,
                period = period,
                title = applicationContext.stringResource(MR.strings.statistics_year_recap_notification_title, label),
                text = applicationContext.stringResource(
                    MR.strings.statistics_year_recap_notification_text,
                    applicationContext.statisticsDurationFormatter()(summary.durationMillis),
                ),
            )
        }
        preferences.lastYearRecapNotification.set(period.editionKey)
    }

    /** The period's summary; null when nothing was timed, so an empty period gets no notification. */
    private suspend fun summaryOf(profileId: Long, period: StatisticsRecapPeriod): StatisticsRecapSummary? {
        val activity = Injekt.get<StatisticsRecapRepository>()
            .getActivity(profileId, period.start.toString(), period.end.toString())
        if (activity.segments.isEmpty()) return null
        return buildStatisticsRecapStory(
            period = period,
            activity = activity,
            previous = null,
            hiddenEntryIds = emptySet(),
            contributions = Injekt.get<EntryStatisticsFeature>().contributions,
        ).summary
    }

    private fun notify(id: Int, profileId: Long, period: StatisticsRecapPeriod, title: String, text: String) {
        val context = applicationContext
        context.notify(id, Notifications.CHANNEL_STATISTICS) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(title)
            setContentText(text)
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setContentIntent(StatisticsRecapNavigation.pendingIntent(context, profileId, period, id))
        }
    }

    companion object {
        private const val TAG = "StatisticsRecapNotification"
        private const val RUN_HOUR = 10

        /** Matches the schedule to the active profile's settings. */
        fun setupTask(context: Context) {
            val profileId = Injekt.get<ActiveProfileProvider>().activeProfileId
            val preferences = StatisticsPreferences(Injekt.get<ProfileStore>().profileStore(profileId))
            if (!preferences.monthlyRecapNotification.get() && !preferences.yearlyRecapNotification.get()) {
                context.workManager.cancelUniqueWork(TAG)
                return
            }
            val now = LocalDateTime.now()
            val nextRun = now.toLocalDate().atTime(RUN_HOUR, 0).let { if (it.isAfter(now)) it else it.plusDays(1L) }
            val request = PeriodicWorkRequestBuilder<StatisticsRecapNotificationJob>(1L, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, nextRun).toMinutes(), TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
            context.workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * Turning a recap on starts with the next one due, so a recap never arrives right away for a period that's
         * already over.
         */
        fun setEnabled(context: Context, profileId: Long, notifications: StatsRecapNotifications) {
            val preferences = StatisticsPreferences(Injekt.get<ProfileStore>().profileStore(profileId))
            if (notifications.monthly && !preferences.monthlyRecapNotification.get()) {
                preferences.lastRecapNotificationMonth.set(YearMonth.now().minusMonths(1L).toString())
            }
            if (notifications.yearly && !preferences.yearlyRecapNotification.get()) {
                newYearRecap(LocalDate.now())?.let { preferences.lastYearRecapNotification.set(it.editionKey) }
            }
            preferences.monthlyRecapNotification.set(notifications.monthly)
            preferences.yearlyRecapNotification.set(notifications.yearly)
            setupTask(context)
        }
    }
}
