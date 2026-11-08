package app.katari.sync.client.trigger

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.client.status.SyncStatusFlow
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tachiyomi.domain.work.WorkOutcome

/** What a sync run started by the platform's background work does, the same on every platform. */
@Inject
class SyncBackgroundRun(
    private val runs: SyncRuns,
    private val connection: SyncConnection,
    private val statusFlow: SyncStatusFlow,
    private val work: SyncBackgroundWork,
    private val preferences: SyncPreferences,
) {
    /**
     * A periodic sync, unless the person turned background sync off or the system restricts background work now;
     * the next period tries again, so it never asks to be retried.
     */
    suspend fun periodic(): WorkOutcome {
        if (!preferences.syncInBackground.get() || !work.allowsBackgroundSync()) return WorkOutcome.Done
        runs.runAndWait(SyncRequest())
        return WorkOutcome.Done
    }

    /**
     * A long run ([app.katari.sync.client.connection.SyncStage.movesLibrary]), to its end, giving [onProgress] the
     * status each time it changes, for the platform to show; retried while the server can't be reached. It waits for
     * the run going now, which may be carrying the library's upload on past the stage that asked for this.
     */
    suspend fun longRun(onProgress: (SyncStatus) -> Unit): WorkOutcome {
        if (!connection.state().stage.movesLibrary && !runs.running.value) return WorkOutcome.Done
        return coroutineScope {
            val reporting = launch { statusFlow.status().collect(onProgress) }
            try {
                when (runs.runAndWait(SyncRequest())) {
                    SyncOutcome.OFFLINE, SyncOutcome.UNREACHABLE, SyncOutcome.FAILED -> WorkOutcome.RetryLater
                    SyncOutcome.SYNCED, SyncOutcome.IDLE, SyncOutcome.WAITING_FOR_WIFI -> WorkOutcome.Done
                }
            } finally {
                reporting.cancel()
            }
        }
    }

    /** Sends what changed; retried while the server can't be reached, until it has it. */
    suspend fun handOff(): WorkOutcome = when (runs.runAndWait(SyncRequest(onlyIfChanged = true))) {
        SyncOutcome.OFFLINE, SyncOutcome.UNREACHABLE, SyncOutcome.FAILED -> WorkOutcome.RetryLater
        SyncOutcome.SYNCED, SyncOutcome.IDLE, SyncOutcome.WAITING_FOR_WIFI -> WorkOutcome.Done
    }
}
