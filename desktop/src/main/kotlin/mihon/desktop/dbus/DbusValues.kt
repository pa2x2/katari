package mihon.desktop.dbus

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The D-Bus wire format of message bodies and header fields, for the types the app's calls use. Values map to Kotlin
 * as: `y` Byte, `b` Boolean, `n`/`q` Short, `i`/`u` Int, `x`/`t` Long, `d` Double, `s`/`o`/`g` String, `v` [Variant],
 * `ay` ByteArray, `a{..}` Map, other arrays List, and structs List.
 */
internal class Variant(val signature: String, val value: Any?)

/** Writes values little-endian, aligned as the specification requires from the start of the message. */
internal class DbusWriter {
    private val bytes = ByteArrayOutputStream()

    val size: Int
        get() = bytes.size()

    fun toByteArray(): ByteArray = bytes.toByteArray()

    fun align(boundary: Int) {
        while (size % boundary != 0) bytes.write(0)
    }

    fun byte(value: Int) = bytes.write(value)

    fun uint32(value: Int) {
        align(4)
        bytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
    }

    fun write(signature: String, value: Any?) {
        when (signature[0]) {
            'y' -> byte((value as Byte).toInt())
            'b' -> uint32(if (value as Boolean) 1 else 0)
            'n', 'q' -> {
                align(2)
                bytes.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value as Short).array())
            }
            'i', 'u' -> uint32(value as Int)
            'x', 't' -> {
                align(8)
                bytes.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value as Long).array())
            }
            'd' -> {
                align(8)
                bytes.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value as Double).array())
            }
            's', 'o' -> string(value as String)
            'g' -> signature(value as String)
            'v' -> {
                val variant = value as Variant
                signature(variant.signature)
                write(variant.signature, variant.value)
            }
            'a' -> array(signature.substring(1), value)
            '(' -> {
                align(8)
                completeTypes(signature.substring(1, signature.length - 1))
                    .zip(value as List<*>)
                    .forEach { (type, field) -> write(type, field) }
            }
            else -> throw IllegalArgumentException("Unsupported D-Bus type $signature")
        }
    }

    private fun string(value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        uint32(encoded.size)
        bytes.write(encoded)
        bytes.write(0)
    }

    private fun signature(value: String) {
        val encoded = value.toByteArray(Charsets.US_ASCII)
        bytes.write(encoded.size)
        bytes.write(encoded)
        bytes.write(0)
    }

    private fun array(element: String, value: Any?) {
        uint32(0)
        val lengthAt = size - 4
        align(alignment(element))
        val start = size
        when {
            element == "y" -> bytes.write(value as ByteArray)
            element.startsWith("{") -> {
                val (keyType, valueType) = completeTypes(element.substring(1, element.length - 1))
                (value as Map<*, *>).forEach { (key, entry) ->
                    align(8)
                    write(keyType, key)
                    write(valueType, entry)
                }
            }
            else -> (value as List<*>).forEach { write(element, it) }
        }
        patchLength(lengthAt, size - start)
    }

    private fun patchLength(at: Int, length: Int) {
        val written = bytes.toByteArray()
        ByteBuffer.wrap(written).order(ByteOrder.LITTLE_ENDIAN).putInt(at, length)
        bytes.reset()
        bytes.write(written)
    }
}

/** Reads values from [buffer], whose position 0 is where alignment counts from: the start of the message. */
internal class DbusReader(private val buffer: ByteBuffer) {
    fun align(boundary: Int) {
        val misalignment = buffer.position() % boundary
        if (misalignment != 0) buffer.position(buffer.position() + boundary - misalignment)
    }

    fun read(signature: String): Any? = when (signature[0]) {
        'y' -> buffer.get()
        'b' -> uint32() != 0
        'n', 'q' -> {
            align(2)
            buffer.getShort()
        }
        'i', 'u' -> uint32()
        'x', 't' -> {
            align(8)
            buffer.getLong()
        }
        'd' -> {
            align(8)
            buffer.getDouble()
        }
        's', 'o' -> string(uint32())
        'g' -> string(buffer.get().toInt() and 0xFF)
        'v' -> {
            val type = read("g") as String
            Variant(type, read(type))
        }
        'a' -> array(signature.substring(1))
        '(' -> {
            align(8)
            completeTypes(signature.substring(1, signature.length - 1)).map(::read)
        }
        else -> throw IllegalArgumentException("Unsupported D-Bus type $signature")
    }

    private fun uint32(): Int {
        align(4)
        return buffer.getInt()
    }

    private fun string(length: Int): String {
        val encoded = ByteArray(length).also(buffer::get)
        buffer.get()
        return String(encoded, Charsets.UTF_8)
    }

    private fun array(element: String): Any {
        val length = uint32()
        align(alignment(element))
        val end = buffer.position() + length
        return when {
            element == "y" -> ByteArray(length).also(buffer::get)
            element.startsWith("{") -> {
                val (keyType, valueType) = completeTypes(element.substring(1, element.length - 1))
                buildMap {
                    while (buffer.position() < end) {
                        align(8)
                        put(read(keyType), read(valueType))
                    }
                }
            }
            else -> buildList { while (buffer.position() < end) add(read(element)) }
        }
    }
}

/** The single complete types [signature] is made of, such as `s`, `a{sv}` and `(oayays)` for `sa{sv}(oayays)`. */
internal fun completeTypes(signature: String): List<String> = buildList {
    var start = 0
    while (start < signature.length) {
        val end = completeTypeEnd(signature, start)
        add(signature.substring(start, end))
        start = end
    }
}

private fun completeTypeEnd(signature: String, start: Int): Int = when (signature[start]) {
    'a' -> completeTypeEnd(signature, start + 1)
    '(', '{' -> {
        var depth = 0
        var index = start
        do {
            when (signature[index]) {
                '(', '{' -> depth++
                ')', '}' -> depth--
            }
            index++
        } while (depth > 0)
        index
    }
    else -> start + 1
}

private fun alignment(signature: String): Int = when (signature[0]) {
    'y', 'g', 'v' -> 1
    'n', 'q' -> 2
    'b', 'i', 'u', 's', 'o', 'a' -> 4
    else -> 8
}
