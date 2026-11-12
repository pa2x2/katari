package app.katari.sync.server.record

import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.protocol.record.SequencedRecord

/** Each account's newest record copies, its sequence and the changes it has applied. */
interface RecordStore {
    /** The newest sequence given out in the account, 0 before its first write. */
    fun sequence(accountId: Long): Long

    /** Records [sequence], past the current one, as the newest given out in the account. */
    fun advanceSequenceTo(accountId: Long, sequence: Long)

    /** The highest sequence of a deletion marker dropped from the account, 0 if none was. */
    fun markersDroppedThrough(accountId: Long): Long

    fun raiseMarkersDroppedThrough(accountId: Long, through: Long)

    /** The sequences of the stored copies of [keys], by key, leaving out the keys the account holds no copy of. */
    fun sequencesOf(accountId: Long, keys: Collection<String>): Map<String, Long>

    /** The sequences the changes were stored with, by change id, leaving out the changes the account has no record of. */
    fun appliedChanges(accountId: Long, changeIds: Collection<String>): Map<String, Long>

    /**
     * Stores [record] as the copy of its key, with the files it lists, remembers its change as applied, and records
     * [chain] as the account's history chain at [sequence]. [replacing] says the account holds a copy of the key
     * already, whose listed files are dropped with it.
     */
    fun write(accountId: Long, record: RecordEnvelope, sequence: Long, chain: ByteArray, replacing: Boolean)

    /** The account's history chain at [sequence], or null when it is not kept. */
    fun chainAt(accountId: Long, sequence: Long): ByteArray?

    /**
     * Stored copies with sequences in ([after], [through]], in sequence order, leaving out the deletion markers at or
     * before [markersAfter].
     */
    fun page(accountId: Long, after: Long, through: Long, limit: Int, markersAfter: Long): List<SequencedRecord>

    /** The stored copies in [group] ([RecordEnvelope.group]), in sequence order, at most [limit]. */
    fun group(accountId: Long, group: String, limit: Int): List<SequencedRecord>

    fun liveTotals(accountId: Long): RecordTotals

    /** Drops the deletion markers up to [through] and returns the newest sequence dropped, or null if none was. */
    fun dropMarkersThrough(accountId: Long, through: Long): Long?

    fun forgetChangesThrough(accountId: Long, through: Long)

    /**
     * Removes every record copy, applied change and history chain value of the account, and starts its history over
     * at [sequence], which must be past its current one, with [chain] as the chain there and nothing before it.
     */
    fun clear(accountId: Long, sequence: Long, chain: ByteArray)

    /**
     * Forgets the history chain before [before], except at the sequences of stored record copies, which a pull from
     * further back still pages through and hands out as positions.
     */
    fun forgetChainBefore(accountId: Long, before: Long)
}

data class RecordTotals(
    val records: Long,
    /** Of payloads, before compression. */
    val payloadBytes: Long,
)
