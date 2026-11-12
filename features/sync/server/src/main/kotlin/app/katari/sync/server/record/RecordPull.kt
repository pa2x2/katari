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
 * A pull that leaves out deletions ([PullRequest.liveOnly]) is a download of the whole account from 0, and still hands
 * out the deletions made after it began: a record an earlier page brought may have been deleted since, and its marker
 * replaced it at a sequence a later page reaches. The download's start is kept with the device, and its position holds
 * those markers back until a page hands them out, like any other device's.
 *
 * A position the history chain is no longer kept for, or a pull whose device needs a deletion marker that was dropped,
 * is refused as [SyncErrorCode.CURSOR_EXPIRED]: the first cannot be checked, and the second would miss that deletion.
 * So is a download past 0 that began before the server kept its start, which may have missed some already. A resync
 * after a lost or restored server ([PullRequest.resync]) is never refused for dropped markers, as its device applied
 * those deletions before.
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
        val deletionsAfter = deletionsNeededAfter(device, request, sequence)
            ?: return@transaction PullOutcome.Refused(SyncErrorCode.CURSOR_EXPIRED)
        if (!request.resync && deletionsAfter < storage.records.markersDroppedThrough(accountId)) {
            return@transaction PullOutcome.Refused(SyncErrorCode.CURSOR_EXPIRED)
        }
        if (request.liveOnly && request.after == 0L) storage.devices.setDownloadStart(device.id, sequence)
        storage.devices.setSyncedUpTo(device.id, request.after)
        val limit = request.limit.takeIf { it in 1..pageSize } ?: pageSize
        val found = storage.records.page(
            accountId = accountId,
            after = request.after,
            through = sequence,
            limit = limit + 1,
            markersAfter = deletionsAfter,
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

    /**
     * The position after which [device] needs every deletion: its own, and for a download also where the download
     * began, or null for one begun before the server kept that. A download starting from 0 begins at the account's
     * [sequence], as every record deleted before it is a marker at or before it, which deletes nothing the device takes.
     */
    private fun deletionsNeededAfter(device: Device, request: PullRequest, sequence: Long): Long? = when {
        !request.liveOnly -> request.after
        request.after == 0L -> sequence
        else -> device.downloadStart?.let { maxOf(it, request.after) }
    }
}

sealed interface PullOutcome {
    data class Page(val page: PullPage) : PullOutcome

    data class Refused(val code: SyncErrorCode) : PullOutcome
}
