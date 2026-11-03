package mihon.desktop.statistics

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.stats.recap.delivery.StatisticsRecapDue
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule
import tachiyomi.domain.work.WorkOutcome
import kotlin.time.Duration.Companion.days

/**
 * The daily check for the active profile's due recaps as a periodic [InProcessWork] schedule, which runs while the app
 * is open and once at launch when the app was closed while it was due (D019), posting each through [notifier]. It is
 * set up while the profile asks for a recap, as Android's daily job is.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessStatisticsRecapSchedule(
    private val work: InProcessWork,
    private val activeProfileProvider: ActiveProfileProvider,
    private val profileStore: ProfileStore,
    private val recapDue: StatisticsRecapDue,
    private val notifier: DesktopStatisticsRecapNotifier,
) : StatisticsRecapSchedule, ResumableWork {
    override val kinds: Set<String> = setOf(NAME)

    override suspend fun setupTask() {
        val preferences = StatisticsPreferences(profileStore.profileStore(activeProfileProvider.activeProfileId))
        if (!preferences.monthlyRecapNotification.get() && !preferences.yearlyRecapNotification.get()) {
            work.cancelPeriodic(NAME)
            return
        }
        work.schedulePeriodic(NAME, 1.days, InProcessWorkRequest(resumeAs = NAME) { deliver() })
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        setupTask()
    }

    private suspend fun deliver(): WorkOutcome = try {
        recapDue.deliver(activeProfileProvider.activeProfileId, notifier::post)
        WorkOutcome.Done
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logcat(LogPriority.ERROR, e) { "Checking for due statistics recaps failed" }
        WorkOutcome.RetryLater
    }

    private companion object {
        // The name of Android's recap work
        const val NAME = "StatisticsRecapNotification"
    }
}
