package mihon.desktop.dbus

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A D-Bus message as the app reads it: what it answers, for a signal where it comes from, and for a call to an object
 * the app serves, who asks and what.
 */
internal class DbusMessage(
    val type: Int,
    val flags: Int,
    val serial: Int,
    val replySerial: Int?,
    val errorName: String?,
    val sender: String?,
    val path: String?,
    val iface: String?,
    val member: String?,
    val signature: String,
    val body: List<Any?>,
) {
    val expectsReply: Boolean
        get() = flags and FLAG_NO_REPLY_EXPECTED == 0

    companion object {
        const val METHOD_CALL = 1
        const val METHOD_RETURN = 2
        const val ERROR = 3
        const val SIGNAL = 4

        /** The bytes before the header fields: byte order, type, flags, version, body length, serial, fields length. */
        const val FIXED_HEADER_SIZE = 16

        private const val FLAG_NO_REPLY_EXPECTED = 1
        private const val PROTOCOL_VERSION = 1
        private const val FIELD_PATH = 1
        private const val FIELD_INTERFACE = 2
        private const val FIELD_MEMBER = 3
        private const val FIELD_ERROR_NAME = 4
        private const val FIELD_REPLY_SERIAL = 5
        private const val FIELD_DESTINATION = 6
        private const val FIELD_SENDER = 7
        private const val FIELD_SIGNATURE = 8

        fun methodCall(
            serial: Int,
            destination: String,
            path: String,
            iface: String,
            member: String,
            signature: String,
            arguments: List<Any?>,
        ): ByteArray = encode(
            type = METHOD_CALL,
            serial = serial,
            fields = listOf(
                FIELD_PATH to Variant("o", path),
                FIELD_INTERFACE to Variant("s", iface),
                FIELD_MEMBER to Variant("s", member),
                FIELD_DESTINATION to Variant("s", destination),
            ),
            signature = signature,
            values = arguments,
        )

        fun methodReturn(serial: Int, call: DbusMessage, signature: String, values: List<Any?>): ByteArray = encode(
            type = METHOD_RETURN,
            serial = serial,
            fields = replyFields(call),
            signature = signature,
            values = values,
        )

        fun error(serial: Int, call: DbusMessage, name: String, message: String): ByteArray = encode(
            type = ERROR,
            serial = serial,
            fields = replyFields(call) + (FIELD_ERROR_NAME to Variant("s", name)),
            signature = "s",
            values = listOf(message),
        )

        /** A signal broadcast to every connection whose match rules let it through. */
        fun signal(
            serial: Int,
            path: String,
            iface: String,
            member: String,
            signature: String,
            values: List<Any?>,
        ): ByteArray = encode(
            type = SIGNAL,
            serial = serial,
            fields = listOf(
                FIELD_PATH to Variant("o", path),
                FIELD_INTERFACE to Variant("s", iface),
                FIELD_MEMBER to Variant("s", member),
            ),
            signature = signature,
            values = values,
        )

        private fun replyFields(call: DbusMessage): List<Pair<Int, Variant>> = buildList {
            add(FIELD_REPLY_SERIAL to Variant("u", call.serial))
            call.sender?.let { add(FIELD_DESTINATION to Variant("s", it)) }
        }

        private fun encode(
            type: Int,
            serial: Int,
            fields: List<Pair<Int, Variant>>,
            signature: String,
            values: List<Any?>,
        ): ByteArray {
            val body = DbusWriter().apply {
                completeTypes(signature).zip(values).forEach { (type, value) -> write(type, value) }
            }.toByteArray()
            val headerFields = buildList {
                fields.forEach { (code, value) -> add(listOf(code.toByte(), value)) }
                if (signature.isNotEmpty()) add(listOf(FIELD_SIGNATURE.toByte(), Variant("g", signature)))
            }
            return DbusWriter().apply {
                byte('l'.code)
                byte(type)
                byte(0)
                byte(PROTOCOL_VERSION)
                uint32(body.size)
                uint32(serial)
                write("a(yv)", headerFields)
                align(8)
            }.toByteArray() + body
        }

        fun parse(message: ByteArray, order: ByteOrder): DbusMessage {
            val buffer = ByteBuffer.wrap(message).order(order)
            val reader = DbusReader(buffer)
            val type = message[1].toInt()
            val flags = message[2].toInt()
            val serial = buffer.getInt(8)
            buffer.position(12)
            val fields = (reader.read("a(yv)") as List<*>).associate { field ->
                val (code, value) = field as List<*>
                (code as Byte).toInt() to (value as Variant).value
            }
            reader.align(8)
            val signature = fields[FIELD_SIGNATURE] as String? ?: ""
            return DbusMessage(
                type = type,
                flags = flags,
                serial = serial,
                replySerial = fields[FIELD_REPLY_SERIAL] as Int?,
                errorName = fields[FIELD_ERROR_NAME] as String?,
                sender = fields[FIELD_SENDER] as String?,
                path = fields[FIELD_PATH] as String?,
                iface = fields[FIELD_INTERFACE] as String?,
                member = fields[FIELD_MEMBER] as String?,
                signature = signature,
                body = completeTypes(signature).map(reader::read),
            )
        }
    }
}
