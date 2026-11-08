package app.katari.sync.server.record

import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.pull.PullPage
import app.katari.sync.protocol.pull.PullRequest
import app.katari.sync.server.device.Device
import app.katari.sync.server.storage.ServerStorage

/**
 * Hands out records after a device's position and records that position as how far the device has synced, which is
 * what holds back the removal of deletion markers it has not seen.
 *
 * A position past the account's sequence, or one whose history chain differs from the one the device was handed
 * there, is refused as [SyncErrorCode.CURSOR_UNKNOWN]: the device synced with a server that had given out more, or
 * gave its sequences to other changes, so this one lost data or was restored from an old copy (and other devices may
 * have pushed past it since), and the device must resync in full rather than skip or misread what was written here.
 *
 * A position the history chain is no longer kept for, or one from before a dropped deletion marker, is refused as
 * [SyncErrorCode.CURSOR_EXPIRED]: the first cannot be checked, and pulling from the second would miss that deletion
 * (a pull that leaves out deletions has nothing to miss and is never refused for it, nor is a resync after a lost or
 * restored server ([PullRequest.resync]), whose device applied those deletions before).
 */
class RecordPull(
    private val storage: ServerStorage,
    private val pageSize: Int,
) {
    fun pull(device: Device, request: PullRequest): PullOutcome = storage.transaction {
        val accountId = device.accountId
        val sequence = storage.records.sequence(accountId)
        if (request.after > sequence) return@transaction PullOutcome.Refused(SyncErrorCode.CURSOR_UNKNOWN)
        val chainThere = storage.chainAt(accountId, request.after)
            ?: return@transaction PullOutcome.Refused(SyncErrorCode.CURSOR_EXPIRED)
        if (request.afterChain.isNotEmpty() && request.afterChain != chainThere.toHexString()) {
            return@transaction PullOutcome.Refused(SyncErrorCode.CURSOR_UNKNOWN)
        }
        if (!request.liveOnly && !request.resync && request.after < storage.records.markersDroppedThrough(accountId)) {
            return@transaction PullOutcome.Refused(SyncErrorCode.CURSOR_EXPIRED)
        }
        storage.devices.setSyncedUpTo(device.id, request.after)
        val limit = request.limit.takeIf { it in 1..pageSize } ?: pageSize
        val found = storage.records.page(
            accountId = accountId,
            after = request.after,
            through = sequence,
            limit = limit + 1,
            includeDeleted = !request.liveOnly,
        )
        val hasMore = found.size > limit
        val records = found.take(limit)
        // Sequences between the last record and the account's are copies since replaced or left out, so a complete
        // page moves the device to the account's sequence.
        val nextCursor = if (hasMore) records.last().sequence else sequence
        PullOutcome.Page(
            PullPage(
                records = records,
                nextCursor = nextCursor,
                hasMore = hasMore,
                server = storage.identityOf(accountId),
                nextChain = checkNotNull(storage.chainAt(accountId, nextCursor)).toHexString(),
            ),
        )
    }
}

sealed interface PullOutcome {
    data class Page(val page: PullPage) : PullOutcome

    data class Refused(val code: SyncErrorCode) : PullOutcome
}
