package mihon.entry.interactions.durable

/**
 * One owner's consequence of an operation that must survive process death. The operation asks every participant to
 * [prepare] a payload before it commits and stores the payloads with its transaction; a delivery loop later hands each
 * stored payload back to the participant with the same [id] until [deliver] succeeds.
 *
 * [id] and each payload's schema version are persisted with queued records, so neither may change once released.
 */
internal interface EntryDurableParticipant<E : Any> {
    val id: String

    /** Returns null when the event leaves this participant nothing to do. */
    suspend fun prepare(event: E): EntryDurablePayload?

    suspend fun deliver(payload: EntryDurablePayload)

    /** Releases what [prepare] staged when the operation does not commit. */
    suspend fun discard(payload: EntryDurablePayload) = Unit
}

internal data class EntryDurablePayload(
    val schemaVersion: Int,
    val value: String,
) {
    init {
        require(schemaVersion > 0) { "Durable payload schema version must be positive" }
    }
}
