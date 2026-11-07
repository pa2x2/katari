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
 * Sync's background work as [InProcessWork], which runs while the app is open: the periodic sync, and a hand-off that
 * a quit cut short runs again at the next launch. A desktop has no battery saver that asks apps to hold back.
 *
 * [run] is a function because [SyncBackgroundRun] itself needs this work.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessSyncBackgroundWork(
    private val work: InProcessWork,
    private val run: () -> SyncBackgroundRun,
) : SyncBackgroundWork, ResumableWork {
    override val kinds: Set<String> = setOf(PERIODIC, HAND_OFF)

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

    override fun allowsBackgroundSync(): Boolean = true

    override suspend fun resume(kind: String, input: Map<String, String>) {
        when (kind) {
            HAND_OFF -> handOff()
            // The triggers schedule the periodic sync again once the app starts.
            PERIODIC -> Unit
        }
    }

    private companion object {
        // The names of Android's sync work
        const val PERIODIC = "SyncPeriodic"
        const val HAND_OFF = "SyncHandOff"
        val HAND_OFF_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
    }
}
