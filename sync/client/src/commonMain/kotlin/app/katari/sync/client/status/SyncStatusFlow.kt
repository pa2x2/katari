package app.katari.sync.client.status

import app.katari.sync.client.apply.HeldRecords
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.file.transfer.FileTransferRule
import app.katari.sync.client.file.transfer.PendingFiles
import app.katari.sync.client.question.Questions
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.run.RUNS_SETTLE
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.run.SyncStep
import app.katari.sync.client.run.SyncSteps
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.network.NetworkState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * [SyncStatus] as it changes, from the connection, the runs, the network, the outbox, questions, held records and the
 * files waiting to move.
 */
@Inject
class SyncStatusFlow(
    private val connection: SyncConnection,
    private val runs: SyncRuns,
    private val network: NetworkConnectivity,
    private val pending: PendingChanges,
    private val questions: Questions,
    private val held: HeldRecords,
    private val pendingFiles: PendingFiles,
    private val fileRule: FileTransferRule,
    private val steps: SyncSteps,
) {
    fun status(): Flow<SyncStatus> {
        val activity = combine(
            connection.states(),
            runs.running.settled(),
            runs.lastOutcome,
            network.state(),
            shownProgress(),
            ::Inputs,
        )
        val waiting = combine(held.unreadableKinds(), held.failedCount(), ::Held)
        return combine(activity, pending.counts(), questions.open(), waiting, files()) {
                inputs,
                unsent,
                open,
                waiting,
                files,
            ->
            val state = inputs.state
            val activityNow = activityOf(inputs)
            SyncStatus(
                link = linkOf(state),
                activity = activityNow,
                unsent = if (state.isLinked) unsent else 0,
                unsentBecause = unsentReason(state, activityNow).takeIf { state.isLinked && unsent > 0 },
                questions = if (state.isLinked) open else emptyList(),
                heldKinds = if (state.isLinked) waiting.kinds else emptyList(),
                heldAside = if (state.isLinked) waiting.failed else 0,
                files = if (state.isLinked) files else SyncStatus.Files.NONE,
            )
        }.distinctUntilChanged()
    }

    /** Whether runs are going, kept true for [RUNS_SETTLE] after one ends. A new collector sees the current value at once. */
    private fun Flow<Boolean>.settled(): Flow<Boolean> = channelFlow {
        var shown: Boolean? = null
        collectLatest { running ->
            if (!running && shown == true) delay(RUNS_SETTLE)
            shown = running
            send(running)
        }
    }

    private fun files(): Flow<SyncStatus.Files> = combine(
        pendingFiles.uploads(),
        pendingFiles.downloads(),
        pendingFiles.missingOnServer(),
        fileRule.allows(),
    ) { uploads, downloads, missing, allowed ->
        SyncStatus.Files(
            toUpload = uploads,
            toDownload = downloads,
            missingOnServer = missing,
            waitingForWifi = !allowed && uploads + downloads > 0,
        )
    }

    private class Held(val kinds: List<String>, val failed: Long)

    /**
     * How far a download and the run's step got, as the status shows them, changing only as what is shown does and at
     * most every [PROGRESS_EVERY]: a step can move hundreds of times a minute, and a screen showing every change never
     * settles, which accessibility services and UI tests wait for.
     */
    private fun shownProgress(): Flow<ShownProgress> = combine(
        connection.states().map(::downloadPercent).distinctUntilChanged(),
        namedStep(),
        ::ShownProgress,
    )
        .distinctUntilChanged()
        .conflate()
        .transform {
            emit(it)
            delay(PROGRESS_EVERY)
        }
        .distinctUntilChanged()

    /** The run's step, once the run has lasted long enough for it to be named ([SyncSteps.shownFrom]). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun namedStep(): Flow<SyncStep?> = steps.step
        .map { it?.shown() }
        .distinctUntilChanged()
        .transformLatest { step ->
            val wait = steps.shownFrom - Clock.System.now().toEpochMilliseconds()
            if (step != null && wait > 0) {
                emit(null)
                delay(wait)
            }
            emit(step)
        }
        .distinctUntilChanged()

    private data class ShownProgress(val download: Int?, val step: SyncStep?)

    private class Inputs(
        val state: SyncConnectionState,
        val running: Boolean,
        val outcome: SyncOutcome,
        val network: NetworkState,
        val progress: ShownProgress,
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
            // A run trying again doesn't make a server found unreachable any closer: that shows until one answers
            inputs.running && unreachableSince == null ->
                SyncStatus.Activity.Syncing(inputs.progress.download, inputs.progress.step)
            !inputs.network.hasNetwork -> SyncStatus.Activity.Offline
            inputs.outcome == SyncOutcome.FAILED -> SyncStatus.Activity.Failed(state.lastSyncedAt)
            unreachableSince != null -> SyncStatus.Activity.Unreachable(unreachableSince)
            inputs.outcome == SyncOutcome.WAITING_FOR_WIFI && state.stage == SyncStage.DOWNLOADING ->
                SyncStatus.Activity.WaitingForWifi
            state.stage != SyncStage.READY -> SyncStatus.Activity.Syncing(inputs.progress.download)
            else -> SyncStatus.Activity.UpToDate(state.lastSyncedAt)
        }
    }

    private fun downloadPercent(state: SyncConnectionState): Int? {
        if (!state.stage.isDownloading || state.stageTarget <= 0) return null
        return (state.position * 100 / state.stageTarget).coerceIn(0, 100).toInt()
    }

    private fun unsentReason(state: SyncConnectionState, activity: SyncStatus.Activity): SyncStatus.UnsentReason? =
        when {
            state.stopped != null -> SyncStatus.UnsentReason.STOPPED
            activity == SyncStatus.Activity.Offline -> SyncStatus.UnsentReason.NO_CONNECTION
            activity is SyncStatus.Activity.Unreachable -> SyncStatus.UnsentReason.SERVER_UNREACHABLE
            else -> null
        }

    private companion object {
        val PROGRESS_EVERY = 2.seconds
    }
}
