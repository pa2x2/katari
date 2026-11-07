package app.katari.sync.client.merge

import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * The newest timestamp anywhere in [payload], or null when it holds none: when the record was last edited. It is what
 * a deletion is compared with and which copy's untimed fields (a source's metadata) are the newer.
 *
 * Every payload stores its timestamps as [HybridTimestamp]s, so walking the payload through its serializer finds them
 * all, however deeply they are nested, without each kind listing its fields again.
 */
internal fun <P> newestEdit(serializer: KSerializer<P>, payload: P): HybridTimestamp? =
    TimestampCollector().apply { encodeSerializableValue(serializer, payload) }.newest

private class TimestampCollector : AbstractEncoder() {
    var newest: HybridTimestamp? = null
        private set

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer.descriptor == TIMESTAMP) {
            val timestamp = value as HybridTimestamp
            if (newest.let { it == null || timestamp > it }) newest = timestamp
        } else {
            super.encodeSerializableValue(serializer, value)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

    override fun encodeValue(value: Any) = Unit

    override fun encodeNull() = Unit

    private companion object {
        val TIMESTAMP = HybridTimestamp.serializer().descriptor
    }
}

/**
 * [field] of whichever of [a] and [b] was edited last, for fields that carry no timestamp of their own and travel with
 * a record's edits, such as a source's metadata; of the larger value when neither was edited later.
 */
internal fun <P, T> fieldOfNewer(serializer: KSerializer<P>, a: P, b: P, field: (P) -> T): T {
    val aEdited = newestEdit(serializer, a)
    val bEdited = newestEdit(serializer, b)
    return when {
        aEdited == bEdited -> larger(field(a), field(b))
        bEdited == null || (aEdited != null && aEdited > bEdited) -> field(a)
        else -> field(b)
    }
}
