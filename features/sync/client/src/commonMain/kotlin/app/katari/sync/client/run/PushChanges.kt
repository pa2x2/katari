package app.katari.sync.client.run

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.file.transfer.FileUploads
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.protocol.push.PushRequest
import app.katari.sync.protocol.record.RecordEnvelope
import dev.zacsweers.metro.Inject

/**
 * Pushes every change waiting to be sent, in chunks of [CHUNK] in key order. A change keeps its id until the server
 * acknowledges it, and only an acknowledgement marks it sent, so a push retried after its answer was lost is
 * recognised by the server as the one it took (D022) and nothing is stored twice.
 *
 * Records naming custom covers or fonts go after the rest of their chunk, once the server has their files
 * ([FileUploads]); a record whose file waits for Wi-Fi stays unsent with it, holding back nothing else.
 *
 * A push the server says was all it took since the device's position moves the device past it (D064), so a device
 * doesn't pull back what it just sent, such as its whole library after its first sync.
 */
@Inject
class PushChanges(
    private val connection: SyncConnection,
    private val continuity: ServerContinuity,
    private val pending: PendingChanges,
    private val uploads: FileUploads,
) {
    /**
     * Pushes everything once. Returns whether the server refused some change as stale: another device changed the
     * record since, so a pull merges the two and the merged record goes with the next push.
     *
     * A refusal naming no server copy (sequence 0) means the record's last copy was a deletion the server since
     * dropped. On the first push the deletion may not have reached this device yet, which the pull that follows
     * finds out; when it is refused so again after that pull ([afterPull]), the change is pushed as a new record.
     *
     * [onTaken] hears how many records the server took. The files a chunk's records wait for can take far longer to
     * upload than the records themselves, such as a library's custom covers after its server lost them, so
     * [onFiles] hears how many of them are settled, done of total, while they upload.
     */
    suspend fun pushAll(
        client: SyncServerClient,
        afterPull: Boolean,
        onFiles: (done: Long, total: Long) -> Unit = { _, _ -> },
        onTaken: (Int) -> Unit = {},
    ): Boolean {
        var stale = false
        var after = ""
        while (true) {
            val chunk = pending.after(after, CHUNK)
            if (chunk.isEmpty()) return stale
            val (plain, naming) = chunk.partition { it.files.isEmpty() }
            if (push(client, plain, afterPull, onTaken)) stale = true
            if (naming.isNotEmpty()) {
                val files = naming.flatMap { it.files }.distinct()
                var settled = 0L
                val waiting = uploads.ensure(client, files) { count ->
                    settled += count
                    onFiles(settled, files.size.toLong())
                }
                if (push(client, naming.filter { it.files.none(waiting::contains) }, afterPull, onTaken)) stale = true
            }
            after = chunk.last().key
        }
    }

    /** Pushes [records], tells [onTaken] how many the server took, and returns whether it refused some as stale. */
    private suspend fun push(
        client: SyncServerClient,
        records: List<RecordEnvelope>,
        afterPull: Boolean,
        onTaken: (Int) -> Unit,
    ): Boolean {
        if (records.isEmpty()) return false
        var stale = false
        val state = connection.state()
        val response = client.push(PushRequest(records, after = state.position, afterChain = state.chain))
        continuity.check(response.server)
        val sent = records.associateBy { it.changeId }
        val taken = ArrayList<PendingChanges.Taken>()
        response.results.forEach { result ->
            val envelope = sent[result.changeId] ?: return@forEach
            when (result.outcome) {
                PushOutcome.ACCEPTED -> taken += PendingChanges.Taken(envelope.key, envelope.changeId, result.sequence)
                PushOutcome.STALE -> {
                    stale = true
                    if (afterPull && result.sequence == 0L && envelope.basedOn != 0L) pending.rebase(envelope.key)
                }
            }
        }
        pending.taken(taken)
        onTaken(taken.size)
        if (response.caughtUp) connection.passOwnPush(response.server)
        return stale
    }

    private companion object {
        // Well under the server's request limit for any payload this app writes, and small enough that a lost answer
        // costs little to send again.
        const val CHUNK = 200L
    }
}
