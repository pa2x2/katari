package mihon.desktop.dbus

import java.io.Closeable
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Linux: a connection of the app's own to the session bus, for services whose calls must share one connection, such
 * as the Secret Service, whose sessions belong to the connection that opened them, which [Gdbus] can't keep. It speaks
 * the D-Bus wire protocol over the bus's Unix socket and authenticates as the user running the app. Calls block the
 * calling thread until the answer arrives or the timeout passes. One caller at a time, except for [wakeUp].
 */
internal class DbusConnection private constructor(private val channel: SocketChannel) : Closeable {
    private val selector = Selector.open()
    private var serial = 0
    private val signals = ArrayDeque<DbusMessage>()

    init {
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ)
    }

    /**
     * The name the bus gave this connection when it said hello, such as `:1.42`, which services address the
     * connection's answers and signals to.
     */
    val uniqueName: String = call(BUS_NAME, BUS_PATH, BUS_NAME, "Hello").first() as String

    /**
     * Calls [member] of [iface] on [path] at [destination] with [arguments] of [signature], and returns the values of
     * the answer. Throws [DbusException] when the service answers with an error, and [IOException] when the bus
     * doesn't answer within [timeout].
     */
    fun call(
        destination: String,
        path: String,
        iface: String,
        member: String,
        signature: String = "",
        vararg arguments: Any?,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): List<Any?> {
        val callSerial = ++serial
        send(DbusMessage.methodCall(callSerial, destination, path, iface, member, signature, arguments.toList()))
        val deadline = TimeSource.Monotonic.markNow() + timeout
        while (true) {
            val message = receive(deadline) ?: if (deadline.hasPassedNow()) throw DbusTimeoutException() else continue
            when {
                message.replySerial != callSerial -> if (message.type == DbusMessage.SIGNAL) signals += message
                message.type == DbusMessage.ERROR ->
                    throw DbusException(message.errorName.orEmpty(), message.body.firstOrNull() as? String)
                else -> return message.body
            }
        }
    }

    /** Asks the bus to send this connection the messages [rule] matches, such as a service's broadcast signals. */
    fun addMatch(rule: String) {
        call(BUS_NAME, BUS_PATH, BUS_NAME, "AddMatch", "s", rule)
    }

    /**
     * The next signal for this connection, the ones that arrived during [call] first; null when none arrives within
     * [timeout] or [wakeUp] ends the wait sooner. Answers to calls that gave up waiting are dropped.
     */
    fun nextSignal(timeout: Duration): DbusMessage? {
        signals.removeFirstOrNull()?.let { return it }
        val deadline = TimeSource.Monotonic.markNow() + timeout
        val message = receive(deadline) ?: return null
        return message.takeIf { it.type == DbusMessage.SIGNAL }
    }

    /** Ends a wait in [nextSignal] at once, or the next one when none runs; the one call any thread may make. */
    fun wakeUp() {
        selector.wakeup()
    }

    /**
     * The values of the next signal [member] of [iface] that [path] sends, which a match rule added before must let
     * through, or null when none arrives within [timeout].
     */
    fun awaitSignal(path: String, iface: String, member: String, timeout: Duration): List<Any?>? {
        val deadline = TimeSource.Monotonic.markNow() + timeout
        while (true) {
            val queued = signals.firstOrNull { it.path == path && it.iface == iface && it.member == member }
            if (queued != null) {
                signals.remove(queued)
                return queued.body
            }
            val message = receive(deadline) ?: if (deadline.hasPassedNow()) return null else continue
            if (message.type == DbusMessage.SIGNAL) signals += message
        }
    }

    override fun close() {
        selector.close()
        channel.close()
    }

    private fun send(message: ByteArray) {
        val buffer = ByteBuffer.wrap(message)
        while (buffer.hasRemaining()) {
            if (channel.write(buffer) == 0) Thread.onSpinWait()
        }
    }

    /**
     * The next message, or null when none starts arriving before [deadline] or a wait for one is woken earlier. Once a
     * message has started, the rest is read within [MESSAGE_TIMEOUT] whatever [deadline] is, since a message left half
     * read would make every later read start in its middle.
     */
    private fun receive(deadline: TimeSource.Monotonic.ValueTimeMark): DbusMessage? {
        val start = ByteBuffer.allocate(DbusMessage.FIXED_HEADER_SIZE)
        if (read(start) == 0) {
            val remaining = -deadline.elapsedNow()
            if (!remaining.isPositive()) return null
            selector.select(remaining.inWholeMilliseconds.coerceAtLeast(1))
            selector.selectedKeys().clear()
            if (read(start) == 0) return null
        }
        val messageDeadline = TimeSource.Monotonic.markNow() + MESSAGE_TIMEOUT
        val fixed = readFully(start, messageDeadline)
        val order = if (fixed[0] == 'B'.code.toByte()) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val header = ByteBuffer.wrap(fixed).order(order)
        val bodyLength = header.getInt(4)
        val fieldsLength = header.getInt(12)
        val fieldsEnd = DbusMessage.FIXED_HEADER_SIZE + fieldsLength
        val bodyStart = (fieldsEnd + 7) / 8 * 8
        val restLength = bodyStart - DbusMessage.FIXED_HEADER_SIZE + bodyLength
        val rest = readFully(ByteBuffer.allocate(restLength), messageDeadline)
        return DbusMessage.parse(fixed + rest, order)
    }

    private fun read(buffer: ByteBuffer): Int {
        val count = channel.read(buffer)
        if (count < 0) throw IOException("The session bus closed the connection")
        return count
    }

    private fun readFully(buffer: ByteBuffer, deadline: TimeSource.Monotonic.ValueTimeMark): ByteArray {
        while (buffer.hasRemaining()) {
            read(buffer)
            if (!buffer.hasRemaining()) break
            val remaining = -deadline.elapsedNow()
            if (!remaining.isPositive()) throw DbusTimeoutException()
            selector.select(remaining.inWholeMilliseconds.coerceAtLeast(1))
            selector.selectedKeys().clear()
        }
        return buffer.array()
    }

    companion object {
        private val DEFAULT_TIMEOUT = 25.seconds

        // The bus sends a message whole, so the rest of one that has started arrives at once
        private val MESSAGE_TIMEOUT = 5.seconds

        /** The user's session bus, or null where there is none to reach. */
        fun session(): DbusConnection? {
            val socket = sessionBusSocket() ?: return null
            val channel = try {
                SocketChannel.open(StandardProtocolFamily.UNIX)
            } catch (_: IOException) {
                return null
            }
            return try {
                channel.connect(UnixDomainSocketAddress.of(socket))
                authenticate(channel)
                DbusConnection(channel)
            } catch (_: IOException) {
                channel.close()
                null
            } catch (_: DbusException) {
                channel.close()
                null
            }
        }

        // The address lists ways to reach the bus; the first Unix socket with a path is the one a desktop sets up.
        private fun sessionBusSocket(): Path? {
            val address = System.getenv("DBUS_SESSION_BUS_ADDRESS")
            val fromAddress = address?.split(';')
                ?.filter { it.startsWith("unix:") }
                ?.firstNotNullOfOrNull { entry ->
                    entry.removePrefix("unix:").split(',')
                        .firstOrNull { it.startsWith("path=") }
                        ?.removePrefix("path=")
                        ?.let(::unescape)
                        ?.let(Path::of)
                }
            if (fromAddress != null) return fromAddress
            if (address != null) return null
            return System.getenv("XDG_RUNTIME_DIR")?.let { Path.of(it, "bus") }?.takeIf { it.exists() }
        }

        private fun unescape(value: String): String = Regex("%([0-9A-Fa-f]{2})").replace(value) {
            it.groupValues[1].toInt(16).toChar().toString()
        }

        /** SASL EXTERNAL, which the bus checks against the credentials of the socket's other end. */
        private fun authenticate(channel: SocketChannel) {
            val uid = Files.getAttribute(Path.of("/proc/self"), "unix:uid").toString()
            val hexUid = uid.toByteArray(Charsets.US_ASCII).joinToString("") { "%02x".format(it) }
            channel.write(ByteBuffer.wrap("\u0000AUTH EXTERNAL $hexUid\r\n".toByteArray(Charsets.US_ASCII)))
            val answer = readLine(channel)
            if (!answer.startsWith("OK ")) throw IOException("The session bus refused to authenticate: $answer")
            channel.write(ByteBuffer.wrap("BEGIN\r\n".toByteArray(Charsets.US_ASCII)))
        }

        private fun readLine(channel: SocketChannel): String {
            val line = StringBuilder()
            val byte = ByteBuffer.allocate(1)
            while (!line.endsWith("\r\n")) {
                byte.clear()
                if (channel.read(byte) < 0) throw IOException("The session bus closed the connection")
                line.append(byte.get(0).toInt().toChar())
            }
            return line.removeSuffix("\r\n").toString()
        }

        const val BUS_NAME = "org.freedesktop.DBus"
        const val BUS_PATH = "/org/freedesktop/DBus"
    }
}

/** An error a service answered a call with, such as `org.freedesktop.DBus.Error.ServiceUnknown`. */
internal class DbusException(val name: String, message: String?) : Exception("$name: ${message.orEmpty()}")

private class DbusTimeoutException : IOException("The session bus didn't answer in time")
