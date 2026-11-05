package app.katari.sync.server.record

import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.protocol.record.SequencedRecord

/** Each account's newest record copies, its sequence and the changes it has applied. */
interface RecordStore {
    /** The newest sequence given out in the account, 0 before its first write. */
    fun sequence(accountId: Long): Long

    /** Gives out the account's next sequence. */
    fun advanceSequence(accountId: Long): Long

    /** The highest sequence of a deletion marker dropped from the account, 0 if none was. */
    fun markersDroppedThrough(accountId: Long): Long

    fun raiseMarkersDroppedThrough(accountId: Long, through: Long)

    /** The sequence of the stored copy of [key], or null when the account has none. */
    fun sequenceOf(accountId: Long, key: String): Long?

    /** The sequence the change was stored with, or null if the account has no record of applying it. */
    fun appliedChange(accountId: Long, changeId: String): Long?

    /** Replaces the stored copy of [record]'s key, with the files it lists, and remembers its change as applied. */
    fun write(accountId: Long, record: RecordEnvelope, sequence: Long)

    /** Stored copies with sequences in ([after], [through]], in sequence order. */
    fun page(accountId: Long, after: Long, through: Long, limit: Int, includeDeleted: Boolean): List<SequencedRecord>

    fun liveTotals(accountId: Long): RecordTotals

    /** Drops the deletion markers up to [through] and returns the newest sequence dropped, or null if none was. */
    fun dropMarkersThrough(accountId: Long, through: Long): Long?

    fun forgetChangesThrough(accountId: Long, through: Long)
}

data class RecordTotals(
    val records: Long,
    /** Of payloads, before compression. */
    val payloadBytes: Long,
)
