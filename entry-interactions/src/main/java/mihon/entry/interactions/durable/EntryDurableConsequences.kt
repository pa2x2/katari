package mihon.entry.interactions.durable

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal data class EntryDurableRecord(
    val participantId: String,
    val payload: EntryDurablePayload,
)

/** Prepares, delivers and discards the durable consequences of one kind of operation. */
internal class EntryDurableConsequences<E : Any>(participants: Set<EntryDurableParticipant<E>>) {
    // Ordered by id so an operation stores and delivers its records in the same order on every run.
    private val participants = participants.sortedBy { it.id }
    private val participantsById = this.participants.associateBy { it.id }

    init {
        check(participantsById.size == this.participants.size) { "Duplicate durable participant ids" }
    }

    /** Prepares every participant's payload. A failure discards what was already prepared and propagates. */
    suspend fun prepare(event: E): List<EntryDurableRecord> {
        val records = mutableListOf<EntryDurableRecord>()
        try {
            participants.forEach { participant ->
                participant.prepare(event)?.let { records += EntryDurableRecord(participant.id, it) }
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) { discard(records) }
            throw error
        }
        return records
    }

    suspend fun deliver(record: EntryDurableRecord) {
        val participant = requireNotNull(participantsById[record.participantId]) {
            "No durable participant ${record.participantId}"
        }
        participant.deliver(record.payload)
    }

    suspend fun discard(records: List<EntryDurableRecord>) {
        records.asReversed().forEach { record ->
            try {
                participantsById[record.participantId]?.discard(record.payload)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Best effort: a staged leftover that cannot be released here is removed by its owner's orphan cleanup.
            }
        }
    }
}
