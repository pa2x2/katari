package app.katari.sync.client.status

import app.katari.sync.client.apply.HeldRecords
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.question.Questions
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.run.SyncRuns
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.network.NetworkState

/** [SyncStatus] as it changes, from the connection, the runs, the network, the outbox, questions and held records. */
@Inject
class SyncStatusFlow(
    private val connection: SyncConnection,
    private val runs: SyncRuns,
    private val network: NetworkConnectivity,
    private val pending: PendingChanges,
    private val questions: Questions,
    private val held: HeldRecords,
) {
    fun status(): Flow<SyncStatus> {
        val activity = combine(connection.states(), runs.running, runs.lastOutcome, network.state(), ::Inputs)
        return combine(activity, pending.counts(), questions.open(), held.unreadableKinds()) {
                inputs,
                unsent,
                open,
                kinds,
            ->
            val state = inputs.state
            val activityNow = activityOf(inputs)
            SyncStatus(
                link = linkOf(state),
                activity = activityNow,
                unsent = if (state.isLinked) unsent else 0,
                unsentBecause = unsentReason(state, activityNow).takeIf { state.isLinked && unsent > 0 },
                questions = if (state.isLinked) open else emptyList(),
                heldKinds = if (state.isLinked) kinds else emptyList(),
            )
        }.distinctUntilChanged()
    }

    private class Inputs(
        val state: SyncConnectionState,
        val running: Boolean,
        val outcome: SyncOutcome,
        val network: NetworkState,
    )

    private fun linkOf(state: SyncConnectionState): SyncStatus.Link {
        val server = state.server
        return when {
            state.isLinked && server != null -> SyncStatus.Link.Linked(server, state.accountName, state.stopped)
            state.stopped == SyncStop.REMOVED || state.stopped == SyncStop.ACCOUNT_DISABLED ->
                SyncStatus.Link.Ended(state.stopped, server, state.accountName)
            else -> SyncStatus.Link.NotSetUp
        }
    }

    private fun activityOf(inputs: Inputs): SyncStatus.Activity {
        val state = inputs.state
        val unreachableSince = state.unreachableSince
        return when {
            !state.isLinked || state.stopped != null -> SyncStatus.Activity.Idle
            state.stage == SyncStage.CHOOSING -> SyncStatus.Activity.WaitingForChoices
            inputs.running -> SyncStatus.Activity.Syncing(progressOf(state))
            !inputs.network.hasNetwork -> SyncStatus.Activity.Offline
            unreachableSince != null -> SyncStatus.Activity.Unreachable(unreachableSince)
            inputs.outcome == SyncOutcome.WAITING_FOR_WIFI && state.stage == SyncStage.DOWNLOADING ->
                SyncStatus.Activity.WaitingForWifi
            state.stage != SyncStage.READY -> SyncStatus.Activity.Syncing(progressOf(state))
            else -> SyncStatus.Activity.UpToDate(state.lastSyncedAt)
        }
    }

    private fun progressOf(state: SyncConnectionState): Float? {
        if (!state.stage.isDownloading || state.stageTarget <= 0) return null
        return (state.position.toFloat() / state.stageTarget).coerceIn(0f, 1f)
    }

    private fun unsentReason(state: SyncConnectionState, activity: SyncStatus.Activity): SyncStatus.UnsentReason? =
        when {
            state.stopped != null -> SyncStatus.UnsentReason.STOPPED
            activity == SyncStatus.Activity.Offline -> SyncStatus.UnsentReason.NO_CONNECTION
            activity is SyncStatus.Activity.Unreachable -> SyncStatus.UnsentReason.SERVER_UNREACHABLE
            else -> null
        }
}
