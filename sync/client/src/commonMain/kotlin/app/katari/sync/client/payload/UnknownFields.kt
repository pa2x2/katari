package app.katari.sync.client.payload

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.protobuf.ProtoNumber
import okio.Buffer

/**
 * Carries the fields of a payload that this app version has no property for, so rewriting a record a newer version
 * wrote never erases what that version added.
 *
 * kotlinx.serialization's protobuf decoder skips fields it doesn't know and keeps nothing of them, so the stored
 * payload is scanned at the wire level instead: [of] returns every top-level field whose number the payload class
 * doesn't declare, byte for byte, and the rewritten payload is the newly encoded known fields followed by those.
 * Protobuf reads a message followed by more fields as one message holding all of them, so the result is a valid
 * payload for every version. Only top-level fields are carried: a newer version adds data as new top-level fields,
 * never inside a message an older version already reads, where this one would drop it.
 */
internal object UnknownFields {
    fun numbersOf(descriptor: SerialDescriptor): Set<Int> = (0 until descriptor.elementsCount).mapTo(HashSet()) { i ->
        descriptor.getElementAnnotations(i).filterIsInstance<ProtoNumber>().firstOrNull()?.number ?: (i + 1)
    }

    fun of(payload: ByteArray, known: Set<Int>): ByteArray {
        val input = Buffer().write(payload)
        val unknown = Buffer()
        while (!input.exhausted()) {
            val field = Buffer()
            val tag = input.copyVarint(field)
            input.copyValue(wireType = (tag and 7L).toInt(), number = tag ushr 3, into = field)
            if ((tag ushr 3).toInt() !in known) unknown.writeAll(field)
        }
        return unknown.readByteArray()
    }

    private fun Buffer.copyVarint(into: Buffer): Long {
        var value = 0L
        var shift = 0
        while (true) {
            val byte = readByte().toInt()
            into.writeByte(byte)
            value = value or ((byte and 0x7f).toLong() shl shift)
            if (byte and 0x80 == 0) return value
            shift += 7
            require(shift < 64) { "Malformed varint" }
        }
    }

    private fun Buffer.copyValue(wireType: Int, number: Long, into: Buffer) {
        when (wireType) {
            WIRE_VARINT -> copyVarint(into)
            WIRE_FIXED64 -> into.write(this, 8)
            WIRE_LENGTH_DELIMITED -> into.write(this, copyVarint(into))
            WIRE_START_GROUP -> while (true) {
                val tag = copyVarint(into)
                if ((tag and 7L).toInt() == WIRE_END_GROUP) {
                    require(tag ushr 3 == number) { "Mismatched group end" }
                    return
                }
                copyValue((tag and 7L).toInt(), tag ushr 3, into)
            }
            WIRE_FIXED32 -> into.write(this, 4)
            else -> throw IllegalArgumentException("Unknown wire type $wireType")
        }
    }

    private const val WIRE_VARINT = 0
    private const val WIRE_FIXED64 = 1
    private const val WIRE_LENGTH_DELIMITED = 2
    private const val WIRE_START_GROUP = 3
    private const val WIRE_END_GROUP = 4
    private const val WIRE_FIXED32 = 5
}
