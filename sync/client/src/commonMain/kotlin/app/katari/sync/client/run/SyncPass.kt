package app.katari.sync.client.run

import app.katari.sync.client.account.Disconnection
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.file.capture.FileCapture
import app.katari.sync.client.file.transfer.FilePlacement
import app.katari.sync.client.file.transfer.FileUploads
import app.katari.sync.client.firstsync.FirstSyncSteps
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.PendingChanges
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
import mihon.core.platform.network.NetworkConnectivity
import tachiyomi.core.common.util.system.logcat

/**
 * One sync run ([SyncRuns] keeps them to one at a time).
 *
 * A device still joining its account goes on with its first sync ([FirstSyncSteps]), one stored stage after another,
 * and pushes nothing until it has taken the account's records, so what it held before merges with them first. A
 * device whose server turned out lost or restored resyncs in full ([FullResync]) before anything else.
 *
 * A device that is ready runs, in this order:
 * 1. The entries it was asked about first ([EntryHandOff]), so their progress is here before anything else.
 * 2. Every page after its position, then the records held for later, which those pages may have made applicable.
 * 3. The settings and files heard but not recorded yet ([SettingsCapture.flush], [FileCapture.flush]), the outbox
 *    drained, and every change pushed.
 *    Pulling first (D021) means the push is based on the newest copies, so it is rarely refused as stale; one that is
 *    refused is pulled, merged and pushed again, a few rounds at most, then left for the next run. Records naming a
 *    custom cover or font go once the server has the file ([PushChanges]).
 * 4. Files: those queued for upload that no pushed record took along, such as after a full resync, and then the files
 *    the applied records name that aren't in place here yet ([FilePlacement]). A file that can't move now, for want
 *    of Wi-Fi or of an answer, waits for a later run without holding back any record.
 *
 * Every page and every acknowledged change is committed as it arrives, so a run cut off anywhere loses nothing and
 * the next one carries on. A refusal that means the device left its account ends the connection ([Disconnection]);
 * one that means the device must sign in again, or that one side needs an update, stops syncing until then
 * ([SyncStop]) while capture goes on.
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
) {
    suspend fun run(request: SyncRequest): SyncOutcome {
        var outcome = SyncOutcome.FAILED
        try {
            outcome = attempt(request)
            return outcome
        } finally {
            // However the run ended, no entry it was asked to fetch first is left being checked (D052)
            handOff.settle(request.entriesFirst, reached = outcome == SyncOutcome.SYNCED || outcome == SyncOutcome.IDLE)
            if (outcome == SyncOutcome.SYNCED) handOff.pulled()
        }
    }

    private suspend fun attempt(request: SyncRequest): SyncOutcome {
        val state = connection.state()
        if (!state.isLinked) return SyncOutcome.IDLE
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
        val client = clients.of(checkNotNull(state.server), token)
        return try {
            // Also how a server that takes connections but never answers is found within a small call's time limit,
            // before a page of records waits the long ones
            val info = client.info()
            if (state.stopped == SyncStop.SERVER_TOO_OLD && !serverUpdated(info)) return SyncOutcome.IDLE
            stages(client, checkNotNull(state.device), request).also { outcome ->
                connection.reached(synced = outcome == SyncOutcome.SYNCED)
            }
        } catch (e: SyncFailure.Refused) {
            handOff.unreachable(request.entriesFirst)
            refused(e.reason)
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
        SyncStop.SIGN_IN_AGAIN, SyncStop.REMOVED, SyncStop.ACCOUNT_DISABLED -> false
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
                    SyncStage.RECORDING -> firstSync.recordRows()
                    SyncStage.DOWNLOADING -> {
                        if (waitsForWifi(state)) return SyncOutcome.WAITING_FOR_WIFI
                        pull.pullAll(client, device, liveOnly = true)
                        connection.setStage(SyncStage.SETTINGS)
                    }
                    SyncStage.SETTINGS -> firstSync.recordSettings()
                    SyncStage.RESYNC, SyncStage.RESYNC_WITHOUT_DELETIONS -> {
                        pull.pullAll(client, device, liveOnly = state.stage == SyncStage.RESYNC_WITHOUT_DELETIONS)
                        resync.finish(state.stage)
                    }
                    SyncStage.READY -> return exchange(client, device, request)
                }
            } catch (e: ResyncNeeded) {
                // A first download that finds its server changed takes the new one's records from the start instead,
                // and still goes on to the device's settings after.
                val stage = if (state.stage == SyncStage.DOWNLOADING) SyncStage.DOWNLOADING else e.stage
                logcat(LogPriority.WARN) { "The sync server was lost or restored; resyncing in full ($stage)" }
                resync.start(stage, e.server ?: client.snapshot().server)
            }
        }
        logcat(LogPriority.ERROR) { "Sync stages didn't settle" }
        return SyncOutcome.FAILED
    }

    private suspend fun exchange(client: SyncServerClient, device: DeviceId, request: SyncRequest): SyncOutcome {
        if (request.entriesFirst.isNotEmpty()) handOff.fetchFirst(client, device, request.entriesFirst)
        pull.pullAll(client, device, liveOnly = false)
        incoming.retryHeld()
        run rounds@{
            repeat(PUSH_ROUNDS) { round ->
                settings.flush()
                fileCapture.flush()
                drainAll()
                if (!push.pushAll(client, afterPull = round > 0)) return@rounds
                pull.pullAll(client, device, liveOnly = false)
            }
        }
        moveFiles(client)
        return SyncOutcome.SYNCED
    }

    private suspend fun moveFiles(client: SyncServerClient) {
        try {
            uploads.offerQueued(client)
            placement.place(client)
        } catch (e: SyncFailure.Refused) {
            throw e
        } catch (e: SyncFailure) {
            // The records are synced; the files wait for the next run.
            logcat(LogPriority.INFO, e) { "Synced files not moved" }
        }
    }

    private suspend fun refused(reason: Refusal): SyncOutcome {
        when (reason) {
            Refusal.TokenNotAccepted -> {
                connection.forgetToken()
                connection.stop(SyncStop.SIGN_IN_AGAIN)
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
        return pending.count() > 0
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
