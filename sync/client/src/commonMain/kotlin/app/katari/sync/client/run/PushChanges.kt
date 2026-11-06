package app.katari.sync.client.run

import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.protocol.push.PushRequest
import dev.zacsweers.metro.Inject

/**
 * Pushes every change waiting to be sent, in chunks of [CHUNK] in key order. A change keeps its id until the server
 * acknowledges it, and only an acknowledgement marks it sent, so a push retried after its answer was lost is
 * recognised by the server as the one it took (D022) and nothing is stored twice.
 */
@Inject
class PushChanges(
    private val continuity: ServerContinuity,
    private val pending: PendingChanges,
) {
    /**
     * Pushes everything once. Returns whether the server refused some change as stale: another device changed the
     * record since, so a pull merges the two and the merged record goes with the next push.
     *
     * A refusal naming no server copy (sequence 0) means the record's last copy was a deletion the server since
     * dropped. On the first push the deletion may not have reached this device yet, which the pull that follows
     * finds out; when it is refused so again after that pull ([afterPull]), the change is pushed as a new record.
     */
    suspend fun pushAll(client: SyncServerClient, afterPull: Boolean): Boolean {
        var stale = false
        var after = ""
        while (true) {
            val chunk = pending.after(after, CHUNK)
            if (chunk.isEmpty()) return stale
            val response = client.push(PushRequest(chunk))
            continuity.check(response.server)
            val sent = chunk.associateBy { it.changeId }
            response.results.forEach { result ->
                val envelope = sent[result.changeId] ?: return@forEach
                when (result.outcome) {
                    PushOutcome.ACCEPTED -> pending.taken(envelope.key, envelope.changeId, result.sequence)
                    PushOutcome.STALE -> {
                        stale = true
                        if (afterPull && result.sequence == 0L && envelope.basedOn != 0L) pending.rebase(envelope.key)
                    }
                }
            }
            after = chunk.last().key
        }
    }

    private companion object {
        // Well under the server's request limit for any payload this app writes, and small enough that a lost answer
        // costs little to send again.
        const val CHUNK = 200L
    }
}
