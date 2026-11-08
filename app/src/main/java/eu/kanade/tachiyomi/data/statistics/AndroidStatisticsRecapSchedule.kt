package eu.kanade.tachiyomi.data.statistics

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.enqueueUniquePeriodicWorkFromCurrentClock
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Inject
class AndroidStatisticsRecapSchedule(
    private val application: Application,
    private val activeProfileProvider: ActiveProfileProvider,
    private val profileStore: ProfileStore,
) : StatisticsRecapSchedule {
    override suspend fun setupTask() {
        val profileId = activeProfileProvider.activeProfileId
        val preferences = StatisticsPreferences(profileStore.profileStore(profileId))
        if (!preferences.monthlyRecapNotification.get() && !preferences.yearlyRecapNotification.get()) {
            application.workManager.cancelUniqueWork(TAG)
            return
        }
        val now = LocalDateTime.now()
        val nextRun = now.toLocalDate().atTime(RUN_HOUR, 0).let { if (it.isAfter(now)) it else it.plusDays(1L) }
        val request = PeriodicWorkRequestBuilder<StatisticsRecapNotificationJob>(1L, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, nextRun).toMinutes(), TimeUnit.MINUTES)
            .addTag(TAG)
            .build()
        application.workManager.enqueueUniquePeriodicWorkFromCurrentClock(
            TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val TAG = "StatisticsRecapNotification"
        const val RUN_HOUR = 10
    }
}
