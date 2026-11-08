package app.katari.sync.client.run

import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.connection.DeviceNaming
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.server.Refusal
import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.pull.PullRequest
import dev.zacsweers.metro.Inject

/**
 * Pulls every page after the device's position, oldest first. Each page is applied together with the position it
 * moves the device to ([IncomingRecords.apply]), so a pull cut off anywhere resumes after the last page applied and
 * never applies one twice or half of one.
 *
 * Before applying, each page is checked against the server the position belongs to (D048): a page from another
 * server ([ServerContinuity]), or a refusal of the position as unknown, means the server was lost or restored, and one
 * refused as expired means it no longer keeps the deletions since; either way it throws [ResyncNeeded] and applies
 * nothing.
 */
@Inject
class PullChanges(
    private val connection: SyncConnection,
    private val continuity: ServerContinuity,
    private val incoming: IncomingRecords,
    private val handOff: EntryHandOff,
    private val naming: DeviceNaming,
) {
    /** [onPage] hears where each page applied moved the device, and the account's sequence then. */
    suspend fun pullAll(
        client: SyncServerClient,
        device: DeviceId,
        liveOnly: Boolean,
        onPage: (position: Long, accountSequence: Long) -> Unit = { _, _ -> },
    ) {
        // Each pull says how this device presents itself, so its row in the device list follows app updates
        val description = naming.description()
        while (true) {
            val state = connection.state()
            val request = PullRequest(
                after = state.position,
                liveOnly = liveOnly,
                afterChain = state.chain,
                device = description,
            )
            val page = try {
                client.pull(request)
            } catch (e: SyncFailure.Refused) {
                throw when (e.reason) {
                    Refusal.PositionUnknown -> ResyncNeeded(SyncStage.RESYNC, server = null)
                    Refusal.PositionExpired -> ResyncNeeded(SyncStage.RESYNC_WITHOUT_DELETIONS, server = null)
                    else -> e
                }
            }
            continuity.check(page.server)
            handOff.watching(client, device, page.records) { incoming.apply(page) }
            onPage(page.nextCursor, page.server.sequence)
            if (!page.hasMore) return
        }
    }
}
