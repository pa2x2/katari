package app.katari.sync.client.trigger

import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import dev.zacsweers.metro.Inject
import tachiyomi.domain.work.WorkOutcome

/** What a sync run started by the platform's background work does, the same on every platform. */
@Inject
class SyncBackgroundRun(
    private val runs: SyncRuns,
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

    /** Sends what changed; retried while the server can't be reached, until it has it. */
    suspend fun handOff(): WorkOutcome = when (runs.runAndWait(SyncRequest(onlyIfChanged = true))) {
        SyncOutcome.OFFLINE, SyncOutcome.UNREACHABLE, SyncOutcome.FAILED -> WorkOutcome.RetryLater
        SyncOutcome.SYNCED, SyncOutcome.IDLE, SyncOutcome.WAITING_FOR_WIFI -> WorkOutcome.Done
    }
}
