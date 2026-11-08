package mihon.desktop.sync

import app.katari.sync.client.trigger.SyncBackgroundRun
import app.katari.sync.client.trigger.SyncBackgroundWork
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import tachiyomi.domain.work.WorkBackoff
import kotlin.time.Duration.Companion.seconds

/**
 * Sync's background work as [InProcessWork], which runs while the app is open: the periodic sync, a hand-off that a
 * quit cut short runs again at the next launch, and a long run, whose progress shows in a notification and which the
 * app asks about before quitting cuts it short. A desktop has no battery saver that asks apps to hold back.
 *
 * [run] is a function because [SyncBackgroundRun] itself needs this work.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessSyncBackgroundWork(
    private val work: InProcessWork,
    private val run: () -> SyncBackgroundRun,
    private val longRunNotification: DesktopSyncLongRunNotification,
) : SyncBackgroundWork, ResumableWork {
    override val kinds: Set<String> = setOf(PERIODIC, HAND_OFF, LONG_RUN)

    override suspend fun schedulePeriodic(enabled: Boolean) {
        if (!enabled) {
            work.cancelPeriodic(PERIODIC)
            return
        }
        val request = InProcessWorkRequest(resumeAs = PERIODIC, requiresNetwork = true) { run().periodic() }
        work.schedulePeriodic(PERIODIC, SyncBackgroundWork.PERIOD, request)
    }

    override fun handOff() {
        val request = InProcessWorkRequest(
            resumeAs = HAND_OFF,
            requiresNetwork = true,
            backoff = HAND_OFF_BACKOFF,
        ) { run().handOff() }
        // A newer hand-off sends everything an older one would have.
        work.enqueue(HAND_OFF, ExistingWorkPolicy.Replace, request)
    }

    // The run itself waits for Wi-Fi where a first download must (D022); the work only needs a network
    override fun longRun(onMetered: Boolean) {
        val request = InProcessWorkRequest(resumeAs = LONG_RUN, requiresNetwork = true, backoff = LONG_RUN_BACKOFF) {
            try {
                run().longRun(longRunNotification::show)
            } finally {
                longRunNotification.withdraw()
            }
        }
        // The work already going carries the run on; a second one would only wait for the same run.
        work.enqueue(LONG_RUN, ExistingWorkPolicy.Keep, request)
    }

    override fun dropLongRun() = work.cancel(LONG_RUN)

    override fun allowsBackgroundSync(): Boolean = true

    override suspend fun resume(kind: String, input: Map<String, String>) {
        when (kind) {
            HAND_OFF -> handOff()
            // The triggers schedule these again once the app starts, the long run while the stage still asks for it.
            PERIODIC, LONG_RUN -> Unit
        }
    }

    private companion object {
        // The names of Android's sync work
        const val PERIODIC = "SyncPeriodic"
        const val HAND_OFF = "SyncHandOff"
        val HAND_OFF_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
        const val LONG_RUN = "SyncLongRun"
        val LONG_RUN_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
    }
}
