package app.katari.tts.provider.speechdispatcher.connection

import app.katari.tts.provider.speechdispatcher.protocol.SSIP_LINE_END
import app.katari.tts.provider.speechdispatcher.protocol.SsipBlockAssembler
import app.katari.tts.provider.speechdispatcher.protocol.SsipEvent
import app.katari.tts.provider.speechdispatcher.protocol.SsipProtocolException
import app.katari.tts.provider.speechdispatcher.protocol.SsipReply
import app.katari.tts.provider.speechdispatcher.protocol.ssipMessageBody
import app.katari.tts.provider.speechdispatcher.service.SpeechDispatcherAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread

/**
 * One open connection to Speech Dispatcher. Commands go one at a time and each waits for its reply; a thread reads the
 * socket and hands replies to the waiting command and events to [onEvent]. When the socket closes, from either side,
 * the waiting command fails with [SpeechDispatcherDisconnectedException] and [onClosed] runs once.
 */
internal class SpeechDispatcherConnection private constructor(
    private val channel: SocketChannel,
    private val onEvent: (SsipEvent) -> Unit,
    private val onClosed: (SpeechDispatcherConnection) -> Unit,
) {
    private val commandMutex = Mutex()

    @Volatile
    private var pendingReply: CompletableDeferred<SsipReply>? = null

    @Volatile
    var isOpen: Boolean = true
        private set

    private fun startReading() {
        thread(isDaemon = true, name = "speech-dispatcher-reader") { read() }
    }

    suspend fun command(command: String): SsipReply = commandMutex.withLock { send(command + SSIP_LINE_END) }

    /** Queues [text] for speech; a successful reply carries the new message's id as its data. */
    suspend fun speak(text: String): SsipReply = commandMutex.withLock {
        val ready = send("SPEAK$SSIP_LINE_END")
        if (ready.code != RECEIVING_DATA) return@withLock ready
        send(ssipMessageBody(text))
    }

    fun close() {
        isOpen = false
        channel.close()
    }

    private suspend fun send(text: String): SsipReply {
        if (!isOpen) throw SpeechDispatcherDisconnectedException()
        val reply = CompletableDeferred<SsipReply>()
        pendingReply = reply
        withContext(Dispatchers.IO) {
            val bytes = ByteBuffer.wrap(text.encodeToByteArray())
            try {
                while (bytes.hasRemaining()) channel.write(bytes)
            } catch (error: IOException) {
                close()
                throw SpeechDispatcherDisconnectedException(error)
            }
        }
        return withTimeoutOrNull(REPLY_TIMEOUT_MILLIS) { reply.await() }
            ?: run {
                // A reply that never came leaves the conversation out of step, so the connection can't be trusted.
                close()
                throw SpeechDispatcherDisconnectedException()
            }
    }

    private fun read() {
        val assembler = SsipBlockAssembler()
        try {
            Channels.newInputStream(channel).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    when (val block = assembler.accept(line)) {
                        is SsipReply -> pendingReply?.complete(block)
                        is SsipEvent -> onEvent(block)
                        null -> Unit
                    }
                }
            }
        } catch (_: IOException) {
            // The socket closed under the reader; what follows treats it as the end of the connection.
        } catch (_: SsipProtocolException) {
            // Past a line it can't read, the reader can no longer tell replies apart.
        } finally {
            close()
            pendingReply?.completeExceptionally(SpeechDispatcherDisconnectedException())
            onClosed(this)
        }
    }

    companion object {
        /** Throws [IOException] when nothing accepts the connection within [CONNECT_TIMEOUT_MILLIS]. */
        suspend fun open(
            address: SpeechDispatcherAddress,
            onEvent: (SsipEvent) -> Unit,
            onClosed: (SpeechDispatcherConnection) -> Unit,
        ): SpeechDispatcherConnection {
            val socketAddress = when (address) {
                is SpeechDispatcherAddress.UnixSocket -> UnixDomainSocketAddress.of(address.path)
                is SpeechDispatcherAddress.InetSocket -> InetSocketAddress(address.host, address.port)
            }
            val channel = withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) {
                runInterruptible(Dispatchers.IO) { SocketChannel.open(socketAddress) }
            } ?: throw IOException("Connection timed out")
            return SpeechDispatcherConnection(channel, onEvent, onClosed).also { it.startReading() }
        }

        private const val RECEIVING_DATA = 230
        private const val REPLY_TIMEOUT_MILLIS = 10_000L
        private const val CONNECT_TIMEOUT_MILLIS = 5_000L
    }
}

internal class SpeechDispatcherDisconnectedException(cause: Throwable? = null) :
    IOException("Speech Dispatcher closed the connection", cause)
