package app.katari.desktop.application.instance

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * What a launch and the running instance say on the instance socket: the launch sends one [InstanceRequest] as JSON
 * and closes its side for writing; the instance answers [ANSWER] once it has taken the request, and closes the
 * connection. A launch that gets no answer has handed nothing over.
 */
internal object InstanceSocketMessages {
    const val ANSWER = "ok"

    // Unit 10's links and files fit in this many times over; a bigger request is not one of the app's
    private const val MAX_MESSAGE_BYTES = 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(request: InstanceRequest): ByteArray = json.encodeToString(request).encodeToByteArray()

    /** The request in [bytes], or null where they hold none. */
    fun decode(bytes: ByteArray): InstanceRequest? = try {
        json.decodeFromString<InstanceRequest>(bytes.decodeToString())
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    fun write(channel: SocketChannel, bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) channel.write(buffer)
    }

    /** Everything the other side sends until it closes its side for writing; a message too big to be one fails. */
    fun readAll(channel: SocketChannel): ByteArray {
        val received = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocate(8192)
        while (true) {
            buffer.clear()
            val count = channel.read(buffer)
            if (count < 0) return received.toByteArray()
            received.write(buffer.array(), 0, count)
            if (received.size() > MAX_MESSAGE_BYTES) throw IOException("The message is larger than any request")
        }
    }
}
