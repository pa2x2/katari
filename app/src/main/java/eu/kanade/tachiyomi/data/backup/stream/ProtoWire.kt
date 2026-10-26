package eu.kanade.tachiyomi.data.backup.stream

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import java.io.IOException

/*
 * The protobuf wire format, as far as walking a backup field by field needs it: a field is a varint tag holding its
 * number and wire type, then a value whose size the wire type fixes or, for a length-delimited field, a varint length
 * gives.
 */

internal const val WIRE_TYPE_VARINT = 0
internal const val WIRE_TYPE_FIXED64 = 1
internal const val WIRE_TYPE_LENGTH_DELIMITED = 2
internal const val WIRE_TYPE_FIXED32 = 5

internal fun lengthDelimitedTag(fieldNumber: Int): Long =
    fieldNumber.toLong() shl 3 or WIRE_TYPE_LENGTH_DELIMITED.toLong()

/** The bytes read as a backup don't form one: they are cut short, corrupt, or not a backup at all. */
internal class MalformedBackupException(message: String?, cause: Throwable? = null) : IOException(message, cause)

/** Reads fields from [source], counting the bytes read so a message nested in another can be kept to its length. */
internal class ProtoInput(private val source: BufferedSource) {
    var position = 0L
        private set

    fun exhausted(): Boolean = reading { source.exhausted() }

    fun readTag(): Long {
        val tag = readVarint()
        if (tag ushr 3 == 0L) throw MalformedBackupException("Invalid field number 0")
        return tag
    }

    fun readLength(): Long {
        val length = readVarint()
        if (length !in 0..Int.MAX_VALUE) throw MalformedBackupException("Invalid length $length")
        return length
    }

    fun readByteArray(length: Long): ByteArray = reading { source.readByteArray(length) }.also { position += length }

    fun skip(length: Long) {
        reading { source.skip(length) }
        position += length
    }

    /** Copies the value of the field [tag] starts, with the tag in front of it, to [sink]. */
    fun copyField(tag: Long, sink: Buffer) {
        sink.writeVarint(tag)
        when (val wireType = (tag and 0x7).toInt()) {
            WIRE_TYPE_VARINT -> sink.writeVarint(readVarint())
            WIRE_TYPE_FIXED64 -> copy(sink, 8)
            WIRE_TYPE_LENGTH_DELIMITED -> {
                val length = readLength()
                sink.writeVarint(length)
                copy(sink, length)
            }
            WIRE_TYPE_FIXED32 -> copy(sink, 4)
            else -> throw MalformedBackupException("Unsupported wire type $wireType")
        }
    }

    private fun copy(sink: Buffer, length: Long) {
        reading { sink.write(source, length) }
        position += length
    }

    private fun readVarint(): Long {
        var result = 0L
        for (shift in 0 until Long.SIZE_BITS step 7) {
            val byte = reading { source.readByte() }.toInt()
            position++
            result = result or ((byte and 0x7F).toLong() shl shift)
            if ((byte and 0x80) == 0) return result
        }
        throw MalformedBackupException("Malformed varint")
    }

    // A backup cut short ends in the middle of a field, which okio reports as running out of input
    private inline fun <T> reading(block: () -> T): T {
        return try {
            block()
        } catch (e: MalformedBackupException) {
            throw e
        } catch (e: IOException) {
            throw MalformedBackupException(e.message, e)
        }
    }
}

internal fun BufferedSink.writeVarint(value: Long) {
    var remaining = value
    while ((remaining and 0x7FL.inv()) != 0L) {
        writeByte((remaining and 0x7F).toInt() or 0x80)
        remaining = remaining ushr 7
    }
    writeByte(remaining.toInt())
}
