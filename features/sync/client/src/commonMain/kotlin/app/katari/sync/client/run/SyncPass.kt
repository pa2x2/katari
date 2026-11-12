package app.katari.sync.client.run

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.sync.client.account.Disconnection
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.file.capture.FileCapture
import app.katari.sync.client.file.transfer.FilePlacement
import app.katari.sync.client.file.transfer.FileTransferRule
import app.katari.sync.client.file.transfer.FileUploads
import app.katari.sync.client.firstsync.FirstSyncSteps
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.message.MessageInbox
import app.katari.sync.client.message.MessageOutbox
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.ChangeTransferRule
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.record.UngroupedRecords
import app.katari.sync.client.resync.FullResync
import app.katari.sync.client.server.Refusal
import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.client.server.SyncServerClients
import app.katari.sync.client.setting.capture.SettingsCapture
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.server.ServerInfo
import app.katari.sync.protocol.version.ProtocolCompatibility
import app.katari.sync.protocol.version.SyncProtocolVersion
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * One sync run ([SyncRuns] keeps them to one at a time).
 *
 * Nothing runs while the person paused syncing.
 *
 * Messages waiting to go to the account's other devices ([MessageOutbox]) go first, once the server answers, whatever
 * stage the device is at and on any network, so a run that has a long way to go doesn't hold them up.
 *
 * A device still joining its account goes on with its first sync ([FirstSyncSteps]), one stored stage after another,
 * and pushes nothing until it has taken the account's records, so what it held before merges with them first. A
 * device whose server turned out lost or restored resyncs in full ([FullResync]) before anything else.
 *
 * A device that is ready runs, in this order:
 * 1. The messages waiting for it ([MessageInbox]), on any network too. Then the entries it was asked about first
 *    ([EntryHandOff]), so their progress is here before anything else. Where changes wait for Wi-Fi
 *    ([ChangeTransferRule]), the run ends here. Records stored before an update that keeps their groups get them
 *    first ([UngroupedRecords]), a batch at a time, so the first migration applied after it doesn't read every record
 *    while every other write waits.
 * 2. Every page after its position, then the records held for later, which those pages may have made applicable.
 * 3. The settings and files heard but not recorded yet ([SettingsCapture.flush], [FileCapture.flush]), the outbox
 *    drained, and every change pushed.
 *    Pulling first (D021) means the push is based on the newest copies, so it is rarely refused as stale; one that is
 *    refused is pulled, merged and pushed again, a few rounds at most, then left for a later run. Records naming a
 *    custom cover or font go once the server has the file ([PushChanges]).
 * 4. Files: those queued for upload that no pushed record took along, such as after a full resync, and then the files
 *    the applied records name that aren't in place here yet ([FilePlacement]). A file that can't move now, for want
 *    of Wi-Fi or of an answer, waits for a later run without holding back any record.
 *
 * A run that leaves changes or files it could send or fetch now, refused as stale every round or cut off from the
 * server while files moved, ends [SyncOutcome.UNFINISHED], and one is run again later.
 *
 * Every page, and the changes each push had acknowledged, are committed as they arrive, so a run cut off anywhere
 * loses nothing and the next one carries on. Steps that can take a while say how far they got ([SyncSteps]). A
 * refusal that means the device left its account ends the connection ([Disconnection]); one that means the device
 * must sign in again, or that one side needs an update, stops syncing until then ([SyncStop]) while capture goes on.
 */
@Inject
class SyncPass(
    private val connection: SyncConnection,
    private val clients: SyncServerClients,
    private val network: NetworkConnectivity,
    private val preferences: SyncPreferences,
    private val firstSync: FirstSyncSteps,
    private val resync: FullResync,
    private val pull: PullChanges,
    private val push: PushChanges,
    private val incoming: IncomingRecords,
    private val pending: PendingChanges,
    private val settings: SettingsCapture,
    private val fileCapture: FileCapture,
    private val drain: OutboxDrain,
    private val uploads: FileUploads,
    private val placement: FilePlacement,
    private val handOff: EntryHandOff,
    private val disconnection: Disconnection,
    private val steps: SyncSteps,
    private val ungrouped: UngroupedRecords,
    private val messageOutbox: MessageOutbox,
    private val messageInbox: MessageInbox,
    private val changeRule: ChangeTransferRule,
    private val fileRule: FileTransferRule,
    // Taken after the first run that synced: a listener may send messages, and sending needs the runs that need this
    private val syncedListeners: Lazy<Set<SyncedListener>>,
) {
    /**
     * Runs [request], calling [working] once it is found to have work to do with the server: a run that stops before,
     * as one asked for after edits that were sync's own writes does, never reads as syncing.
     */
    suspend fun run(request: SyncRequest, working: () -> Unit = {}): SyncOutcome {
        var outcome = SyncOutcome.FAILED
        try {
            outcome = attempt(request, working)
            return outcome
        } finally {
            // However the run ended, no entry it was asked to fetch first is left being checked (D052)
            handOff.settle(request.entriesFirst, reached = outcome.pulledAll || outcome == SyncOutcome.IDLE)
            if (outcome.pulledAll) {
                handOff.pulled()
                tellSynced()
            }
        }
    }

    private fun tellSynced() {
        syncedListeners.value.forEach { listener ->
            try {
                listener.onSynced()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "A listener to synced runs failed" }
            }
        }
    }

    private suspend fun attempt(request: SyncRequest, working: () -> Unit): SyncOutcome {
        val state = connection.state()
        if (!state.isLinked || preferences.paused.get()) return SyncOutcome.IDLE
        if (!resumes(state)) return SyncOutcome.IDLE
        if (request.onlyIfChanged && request.entriesFirst.isEmpty() && state.stage == SyncStage.READY) {
            if (!hasChanges()) return SyncOutcome.IDLE
        }
        // An entry asked for by opening it is being checked already, or was told unreachable while this run waited
        handOff.checking(request.entriesFirst.filterNot { it in handOff.checks.value })
        if (!network.current().hasNetwork) {
            handOff.unreachable(request.entriesFirst)
            return SyncOutcome.OFFLINE
        }
        val token = connection.token()
        if (token == null) {
            connection.stop(SyncStop.SIGN_IN_AGAIN)
            return SyncOutcome.IDLE
        }
        working()
        val client = clients.of(checkNotNull(state.server), token)
        var info: ServerInfo? = null
        return try {
            // Also how a server that takes connections but never answers is found within a small call's time limit,
            // before a page of records waits the long ones
            info = client.info()
            // A server that answers is reachable from now, not only once the run is done, which a large pull makes long
            connection.reached(synced = false)
            if (state.stopped == SyncStop.SERVER_TOO_OLD && !serverUpdated(info)) return SyncOutcome.IDLE
            messageOutbox.deliver(client)
            stages(client, checkNotNull(state.device), request).also { outcome ->
                connection.reached(synced = outcome == SyncOutcome.SYNCED)
            }
        } catch (e: SyncFailure.Refused) {
            handOff.unreachable(request.entriesFirst)
            // The instance names the server's storage: another one means it started over and the account went with it
            refused(e.reason, serverStartedOver = info != null && info.instanceId != state.instanceId)
        } catch (e: SyncFailure) {
            logcat(LogPriority.INFO, e) { "Sync server not reached" }
            handOff.unreachable(request.entriesFirst)
            connection.unreachable()
            SyncOutcome.UNREACHABLE
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failure here, such as a change that can't be applied, is not the server's: one that answered is
            // reachable, so the run is reported as failed rather than as an unreachable server.
            if (client.answered) connection.reached(synced = false)
            throw e
        }
    }

    /** Whether a device that stopped syncing for [SyncConnectionState.stopped] may try again now. */
    private suspend fun resumes(state: SyncConnectionState): Boolean = when (state.stopped) {
        null, SyncStop.SERVER_TOO_OLD -> true
        SyncStop.APP_TOO_OLD -> (SyncProtocolVersion.CURRENT > state.stoppedProtocol).also { updated ->
            if (updated) connection.stop(null)
        }
        SyncStop.SIGN_IN_AGAIN, SyncStop.SERVER_RESET, SyncStop.REMOVED, SyncStop.ACCOUNT_DISABLED -> false
    }

    private suspend fun serverUpdated(info: ServerInfo): Boolean {
        val compatibility = info.protocolVersions.compatibilityOf(SyncProtocolVersion.CURRENT)
        if (compatibility == ProtocolCompatibility.SUPPORTED) connection.stop(null)
        return compatibility == ProtocolCompatibility.SUPPORTED
    }

    private suspend fun stages(client: SyncServerClient, device: DeviceId, request: SyncRequest): SyncOutcome {
        repeat(MAX_STAGES) {
            val state = connection.state()
            try {
                when (state.stage) {
                    SyncStage.JOINING -> firstSync.join(client)
                    SyncStage.CHOOSING -> return SyncOutcome.IDLE
                    SyncStage.RECORDING -> steps.step(SyncStep.Kind.PREPARING) { firstSync.recordRows(it) }
                    SyncStage.RECORDING_KEPT -> steps.step(SyncStep.Kind.PREPARING) { firstSync.recordKept(it) }
                    SyncStage.DOWNLOADING -> {
                        if (waitsForWifi(state)) return SyncOutcome.WAITING_FOR_WIFI
                        pull.pullAll(client, device, liveOnly = true)
                        resync.finishDownload(state.downloadRestartedAs)
                    }
                    SyncStage.SETTINGS -> firstSync.recordSettings()
                    SyncStage.RESYNC, SyncStage.RESYNC_WITHOUT_DELETIONS -> {
                        pull.pullAll(client, device, liveOnly = state.stage == SyncStage.RESYNC_WITHOUT_DELETIONS)
                        resync.finish(state.stage)
                    }
                    SyncStage.READY -> return exchange(client, device, request)
                }
            } catch (e: ResyncNeeded) {
                val server = e.server ?: client.snapshot().server
                // A position refused by another instance means nothing there, whatever the refusal said
                val reason = if (state.instanceId != null && server.instanceId != state.instanceId) {
                    SyncStage.RESYNC
                } else {
                    e.stage
                }
                logcat(LogPriority.WARN) { "The sync server can't continue from this device's position ($reason)" }
                when (state.stage) {
                    // A first download takes the server's records from the start instead, and still goes on to the
                    // device's settings after
                    SyncStage.DOWNLOADING -> resync.restartDownload(reason, server)
                    // A resync after a lost or restored server stays one: resyncing without deletions deletes what the
                    // server doesn't send back, which is what it lost
                    SyncStage.RESYNC -> resync.start(SyncStage.RESYNC, server)
                    else -> resync.start(reason, server)
                }
            }
        }
        logcat(LogPriority.ERROR) { "Sync stages didn't settle" }
        return SyncOutcome.FAILED
    }

    private suspend fun exchange(client: SyncServerClient, device: DeviceId, request: SyncRequest): SyncOutcome {
        messageInbox.fetch(client)
        ungrouped.fill()
        if (request.entriesFirst.isNotEmpty()) handOff.fetchFirst(client, device, request.entriesFirst)
        if (!changeRule.allowsNow()) return SyncOutcome.WAITING_FOR_WIFI
        steps.step(SyncStep.Kind.DOWNLOADING_CHANGES) { progress ->
            val from = connection.state().position
            pull.pullAll(client, device, liveOnly = false) { position, sequence ->
                progress(position - from, sequence - from)
            }
        }
        incoming.retryHeld()
        var stale = false
        steps.step(SyncStep.Kind.UPLOADING_RECORDS) { progress ->
            var sent = 0L
            // The files records wait for, named while they upload, the records named again once taken
            val files = steps.progressOf(SyncStep.Kind.UPLOADING_FILES)
            run rounds@{
                repeat(PUSH_ROUNDS) { round ->
                    settings.flush()
                    fileCapture.flush()
                    drainAll()
                    val total = sent + pending.count()
                    progress(sent, total)
                    stale = push.pushAll(client, afterPull = round > 0, onFiles = files) { taken ->
                        sent += taken
                        progress(sent, total)
                    }
                    if (!stale) return@rounds
                    pull.pullAll(client, device, liveOnly = false)
                }
            }
        }
        val filesMoved = moveFiles(client)
        // A change still waiting waits for nothing a later run lacks when the server kept refusing it as stale, or when
        // files may move, as one naming a file whose bytes changed while they were uploaded
        val changesLeft = pending.count() > 0 && (stale || fileRule.allowsNow())
        return if (filesMoved && !changesLeft) SyncOutcome.SYNCED else SyncOutcome.UNFINISHED
    }

    /** Returns false when the server stopped answering midway. */
    private suspend fun moveFiles(client: SyncServerClient): Boolean {
        try {
            steps.step(SyncStep.Kind.UPLOADING_FILES) { progress -> uploads.offerQueued(client, progress) }
            steps.step(SyncStep.Kind.DOWNLOADING_FILES) { progress -> placement.place(client, progress) }
            return true
        } catch (e: SyncFailure.Refused) {
            throw e
        } catch (e: SyncFailure) {
            // The records are synced; the files wait for the run tried again.
            logcat(LogPriority.INFO, e) { "Synced files not moved" }
            return false
        }
    }

    private suspend fun refused(reason: Refusal, serverStartedOver: Boolean): SyncOutcome {
        when (reason) {
            Refusal.TokenNotAccepted -> {
                connection.forgetToken()
                connection.stop(if (serverStartedOver) SyncStop.SERVER_RESET else SyncStop.SIGN_IN_AGAIN)
            }
            Refusal.DeviceRemoved -> disconnection.leave(SyncStop.REMOVED)
            Refusal.AccountDisabled -> disconnection.leave(SyncStop.ACCOUNT_DISABLED)
            is Refusal.Incompatible -> connection.stop(
                if (reason.compatibility == ProtocolCompatibility.APP_TOO_OLD) {
                    SyncStop.APP_TOO_OLD
                } else {
                    SyncStop.SERVER_TOO_OLD
                },
            )
            Refusal.NotAccepted, is Refusal.Throttled, Refusal.UsernameTaken, Refusal.PositionExpired,
            Refusal.PositionUnknown,
            -> {
                logcat(LogPriority.WARN) { "Sync refused unexpectedly: $reason" }
                return SyncOutcome.FAILED
            }
        }
        return SyncOutcome.IDLE
    }

    private suspend fun waitsForWifi(state: SyncConnectionState): Boolean =
        state.stageTarget > 0 && !preferences.useMobileData.get() && !network.current().countsAsWifi

    private suspend fun hasChanges(): Boolean {
        settings.flush()
        fileCapture.flush()
        drainAll()
        return pending.count() > 0 || messageOutbox.count() > 0
    }

    private suspend fun drainAll() {
        do {
            val drained = drain.drain()
        } while (drained > 0)
    }

    private companion object {
        // Joining, choosing or recording, downloading, settings, ready, and a resync or two on the way.
        const val MAX_STAGES = 10
        const val PUSH_ROUNDS = 3
    }
}
