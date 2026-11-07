package app.katari.sync.server.record

import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.protocol.push.PushRequest
import app.katari.sync.protocol.push.PushResponse
import app.katari.sync.protocol.push.PushResult
import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.server.device.Device
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.storage.ServerStorage

/**
 * Takes in pushed records. A record is accepted only when it was made on the copy the server holds: its
 * [RecordEnvelope.basedOn] is that copy's sequence, or 0 for a key the server holds nothing for. That needs no look
 * inside the payload. Otherwise it is refused as stale with the server's sequence, and the device pulls, merges and
 * pushes again.
 *
 * A key with no copy refuses every record based on anything but 0, the case of an edit made on a deletion marker
 * retention has since dropped: accepting it would put the edit over the deletion without the device ever merging the
 * two, so the device is told the server holds nothing (sequence 0) and merges first.
 *
 * A change the account has already applied is answered as accepted with the sequence it got then, so a push retried
 * after its answer was lost changes nothing.
 *
 * A push from a device at the account's newest position is answered as [PushResponse.caughtUp]: the writes are one
 * transaction, so every sequence past that position is one of its own records.
 */
class RecordPush(
    private val storage: ServerStorage,
    private val changes: AccountChanges,
) {
    fun push(device: Device, request: PushRequest): PushResponse {
        val accountId = device.accountId
        var wrote = false
        val response = storage.transaction {
            val before = storage.identityOf(accountId)
            val caughtUp = request.after == before.sequence &&
                (request.afterChain.isEmpty() || request.afterChain == before.chain)
            // Looked up for the whole push at once, then kept up to date with its own writes, which a later record
            // with the same key or change id sees.
            val applied = storage.records.appliedChanges(accountId, request.records.map { it.changeId }).toMutableMap()
            val stored = storage.records.sequencesOf(accountId, request.records.map { it.key }).toMutableMap()
            var sequence = before.sequence
            var chain = checkNotNull(storage.chainAt(accountId, sequence))
            val results = request.records.map { record ->
                applied[record.changeId]?.let { return@map PushResult(record.changeId, PushOutcome.ACCEPTED, it) }
                val current = stored[record.key] ?: 0
                if (record.basedOn != current) return@map PushResult(record.changeId, PushOutcome.STALE, current)
                sequence++
                chain = HistoryChain.next(chain, sequence, record.key, record.changeId)
                // Stamped with the device that pushed it, whatever the envelope claims.
                storage.records.write(accountId, record.copy(device = device.id), sequence, chain, current != 0L)
                applied[record.changeId] = sequence
                stored[record.key] = sequence
                PushResult(record.changeId, PushOutcome.ACCEPTED, sequence)
            }
            wrote = sequence != before.sequence
            if (wrote) storage.records.advanceSequenceTo(accountId, sequence)
            PushResponse(results = results, server = storage.identityOf(accountId), caughtUp = caughtUp)
        }
        if (wrote) changes.announce(accountId, response.server.sequence)
        return response
    }
}
