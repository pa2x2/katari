package app.katari.sync.client.firstsync

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.server.SyncServerClient
import dev.zacsweers.metro.Inject

/**
 * Runs a device's first sync in [FirstSync]'s order, one stage of [SyncStage] at a time, each stage stored once done,
 * so a first sync interrupted anywhere, by the app being killed or the network dropping, carries on from that stage.
 * Capture started when the device signed in, so edits made meanwhile are captured as edits.
 *
 * - [join]: the first device of an empty account, and a device with no data of its own (D047), pair as proposed and
 *   take the account's settings without asking; any other device waits at [SyncStage.CHOOSING] for [choose].
 * - [recordRows]: the device's rows, custom covers and fonts, before anything of the account's is applied.
 * - The account's records come by the run's pull, a page at a time, each committed with the position.
 * - [recordSettings]: the device's preferences, after the account's settings are in place.
 */
@Inject
class FirstSyncSteps(
    private val firstSync: FirstSync,
    private val connection: SyncConnection,
    private val rows: ExistingRows,
    private val existingFiles: ExistingFiles,
    private val existingSettings: ExistingSettings,
) {
    suspend fun join(client: SyncServerClient) {
        val snapshot = client.snapshot()
        val accountIsEmpty = snapshot.recordCount == 0L
        if (accountIsEmpty || !firstSync.hasOwnData()) {
            val pairings = firstSync.proposal(if (accountIsEmpty) emptyList() else accountProfiles(client))
            firstSync.pair(pairings.map { ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.ACCOUNT) })
            connection.setStage(SyncStage.RECORDING, target = snapshot.server.sequence)
        } else {
            connection.setStage(SyncStage.CHOOSING, target = snapshot.server.sequence)
        }
    }

    /** This device's profiles, each paired by name with the account's where one matches, for the person to confirm. */
    suspend fun proposal(client: SyncServerClient): List<ProfilePairing> = firstSync.proposal(accountProfiles(client))

    /** Takes the person's [choices] for a device waiting at [SyncStage.CHOOSING]. */
    suspend fun choose(choices: List<ProfileChoice>) {
        val state = connection.state()
        check(state.stage == SyncStage.CHOOSING) { "Not waiting for first-sync choices" }
        firstSync.pair(choices)
        connection.setStage(SyncStage.RECORDING, target = state.stageTarget)
    }

    suspend fun recordRows() {
        existingFiles.record()
        rows.record()
        connection.setStage(SyncStage.DOWNLOADING, target = connection.state().stageTarget)
    }

    suspend fun recordSettings() {
        existingSettings.record()
        connection.setStage(SyncStage.READY)
    }

    private suspend fun accountProfiles(client: SyncServerClient) =
        client.group(RecordGroups.PROFILES).records.map { it.record }
}
