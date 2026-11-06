package app.katari.sync.client.record

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp

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
