package mihon.desktop.dbus

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A D-Bus message as the app reads it: what it answers, or for a signal, where it comes from. */
internal class DbusMessage(
    val type: Int,
    val replySerial: Int?,
    val errorName: String?,
    val path: String?,
    val iface: String?,
    val member: String?,
    val body: List<Any?>,
) {
    companion object {
        const val METHOD_CALL = 1
        const val ERROR = 3
        const val SIGNAL = 4

        /** The bytes before the header fields: byte order, type, flags, version, body length, serial, fields length. */
        const val FIXED_HEADER_SIZE = 16

        private const val PROTOCOL_VERSION = 1
        private const val FIELD_PATH = 1
        private const val FIELD_INTERFACE = 2
        private const val FIELD_MEMBER = 3
        private const val FIELD_ERROR_NAME = 4
        private const val FIELD_REPLY_SERIAL = 5
        private const val FIELD_DESTINATION = 6
        private const val FIELD_SIGNATURE = 8

        fun methodCall(
            serial: Int,
            destination: String,
            path: String,
            iface: String,
            member: String,
            signature: String,
            arguments: List<Any?>,
        ): ByteArray {
            val body = DbusWriter().apply {
                completeTypes(signature).zip(arguments).forEach { (type, argument) -> write(type, argument) }
            }.toByteArray()
            val fields = buildList {
                add(listOf(FIELD_PATH.toByte(), Variant("o", path)))
                add(listOf(FIELD_INTERFACE.toByte(), Variant("s", iface)))
                add(listOf(FIELD_MEMBER.toByte(), Variant("s", member)))
                add(listOf(FIELD_DESTINATION.toByte(), Variant("s", destination)))
                if (signature.isNotEmpty()) add(listOf(FIELD_SIGNATURE.toByte(), Variant("g", signature)))
            }
            return DbusWriter().apply {
                byte('l'.code)
                byte(METHOD_CALL)
                byte(0)
                byte(PROTOCOL_VERSION)
                uint32(body.size)
                uint32(serial)
                write("a(yv)", fields)
                align(8)
            }.toByteArray() + body
        }

        fun parse(message: ByteArray, order: ByteOrder): DbusMessage {
            val buffer = ByteBuffer.wrap(message).order(order)
            val reader = DbusReader(buffer)
            val type = message[1].toInt()
            buffer.position(12)
            val fields = (reader.read("a(yv)") as List<*>).associate { field ->
                val (code, value) = field as List<*>
                (code as Byte).toInt() to (value as Variant).value
            }
            reader.align(8)
            val signature = fields[FIELD_SIGNATURE] as String? ?: ""
            return DbusMessage(
                type = type,
                replySerial = fields[FIELD_REPLY_SERIAL] as Int?,
                errorName = fields[FIELD_ERROR_NAME] as String?,
                path = fields[FIELD_PATH] as String?,
                iface = fields[FIELD_INTERFACE] as String?,
                member = fields[FIELD_MEMBER] as String?,
                body = completeTypes(signature).map(reader::read),
            )
        }
    }
}
