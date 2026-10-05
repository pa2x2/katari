package eu.kanade.tachiyomi.data.statistics

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.presentation.more.stats.components.statisticsDurationFormatter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapNavigation
import eu.kanade.tachiyomi.ui.stats.recap.buildStatisticsRecap
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.repository.StatisticsRepository
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Posts the active profile's recap of the previous month once that month is over.
 *
 * Runs daily rather than on the 1st, so a device that is off or idle that day still gets the recap later.
 */
class StatisticsRecapNotificationJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val profileId = Injekt.get<ActiveProfileProvider>().activeProfileId
        val preferences = StatisticsPreferences(Injekt.get<ProfileStore>().profileStore(profileId))
        if (!preferences.monthlyRecapNotification.get()) return Result.success()

        val month = YearMonth.now().minusMonths(1L)
        if (preferences.lastRecapNotificationMonth.get() >= month.toString()) return Result.success()

        return try {
            notifyRecap(profileId, month)
            preferences.lastRecapNotificationMonth.set(month.toString())
            Result.success()
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error)
            Result.retry()
        }
    }

    private suspend fun notifyRecap(profileId: Long, month: YearMonth) {
        val start = month.atDay(1).toString()
        val end = month.atEndOfMonth().toString()
        val snapshot = Injekt.get<StatisticsRepository>().subscribeActivity(profileId, start, end).first()
        val recap = buildStatisticsRecap(snapshot, type = null)
        if (recap.totalDurationMillis <= 0L) return

        val context = applicationContext
        val locale = Locale.getDefault()
        val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        val periodLabel = "$monthName ${month.year}"
        val completions = recap.completionCount.toInt()
        context.notify(Notifications.ID_STATISTICS_RECAP, Notifications.CHANNEL_STATISTICS) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(
                context.stringResource(
                    MR.strings.statistics_recap_notification_title,
                    monthName,
                    context.statisticsDurationFormatter()(recap.totalDurationMillis),
                ),
            )
            setContentText(
                context.stringResource(
                    MR.strings.statistics_recap_notification_text,
                    context.pluralStringResource(MR.plurals.statistics_completion_count, completions, completions),
                    context.pluralStringResource(
                        MR.plurals.statistics_active_day_count,
                        recap.activeDays,
                        recap.activeDays,
                    ),
                ),
            )
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setContentIntent(StatisticsRecapNavigation.pendingIntent(context, profileId, start, end, periodLabel))
        }
    }

    companion object {
        private const val TAG = "StatisticsRecapNotification"
        private const val RUN_HOUR = 10

        /** Matches the schedule to the active profile's setting. */
        fun setupTask(context: Context) {
            val profileId = Injekt.get<ActiveProfileProvider>().activeProfileId
            val preferences = StatisticsPreferences(Injekt.get<ProfileStore>().profileStore(profileId))
            if (!preferences.monthlyRecapNotification.get()) {
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
         * Turning the recap on starts with the month after the one that just ended, so a recap never arrives right
         * away for a month that's already over.
         */
        fun setEnabled(context: Context, profileId: Long, enabled: Boolean) {
            val preferences = StatisticsPreferences(Injekt.get<ProfileStore>().profileStore(profileId))
            if (enabled == preferences.monthlyRecapNotification.get()) return
            if (enabled) preferences.lastRecapNotificationMonth.set(YearMonth.now().minusMonths(1L).toString())
            preferences.monthlyRecapNotification.set(enabled)
            setupTask(context)
        }
    }
}
