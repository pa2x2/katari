package app.katari.sync.client.record

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId

/**
 * The timestamps of the edits the outbox holds for one record: per payload field, the newest edit that touched it.
 * A row inserted or deleted touches every field.
 */
class FieldStamps internal constructor() {
    private val byField = HashMap<Int, HybridTimestamp>()
    private var everyField: HybridTimestamp? = null

    /** The newest edit of the record, or null when the outbox held none for it. */
    var latest: HybridTimestamp? = null
        private set

    internal fun add(fields: Long, stamp: HybridTimestamp) {
        if (fields == EVERY_FIELD) {
            everyField = newer(everyField, stamp)
        } else {
            (1 until Long.SIZE_BITS).filter { fields and (1L shl it) != 0L }.forEach { number ->
                byField[number] = newer(byField[number], stamp)!!
            }
        }
        latest = newer(latest, stamp)
    }

    /**
     * [value] as the record's field [number] now holds it. An unchanged value keeps its timestamp; a changed one
     * takes the newest edit that touched the field, or keeps the old timestamp when no captured edit did (a value
     * every device rewrites alike, such as a migration's).
     */
    fun <T> field(number: Int, value: T, previous: Stamped<T>?): Stamped<T> {
        if (previous != null && previous.value == value) return previous
        val edited = newer(byField[number], everyField)
        return Stamped(value, edited ?: previous?.at ?: Stamped.UNKNOWN_EDIT)
    }

    /**
     * [field] for a value whose row keeps the time it was last changed, [editedAtMillis] (0 for none), as reading
     * positions do: an edit takes that time instead of the time of the write, so a write that carries an older value
     * along with its time, as a position moved to a renamed chapter or copied by a migration does, doesn't pass for
     * new reading and win over newer reading on other devices. A row with no time takes the write's. A value older
     * than the one the record holds leaves the record's, as merging positions on one device keeps the newer.
     */
    fun <T> fieldEditedAt(
        number: Int,
        value: T,
        previous: Stamped<T>?,
        editedAtMillis: Long,
        device: DeviceId,
    ): Stamped<T> {
        val stamped = field(number, value, previous)
        val captured = newer(byField[number], everyField)
        if (captured == null || stamped === previous || editedAtMillis <= 0) return stamped
        val edited = Stamped(value, HybridTimestamp(editedAtMillis, 0, device))
        return if (previous != null && previous.at > edited.at) previous else edited
    }

    private fun newer(a: HybridTimestamp?, b: HybridTimestamp?): HybridTimestamp? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }

    companion object {
        /** `sync_outbox.fields` for a row inserted or deleted. */
        const val EVERY_FIELD: Long = -1
    }
}
