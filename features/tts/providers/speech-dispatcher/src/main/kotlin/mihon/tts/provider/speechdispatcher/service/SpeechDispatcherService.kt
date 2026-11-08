package mihon.tts.provider.speechdispatcher.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.tts.provider.speechdispatcher.availability.SpeechDispatcherProblem
import mihon.tts.provider.speechdispatcher.connection.SpeechDispatcherConnection
import mihon.tts.provider.speechdispatcher.connection.SpeechDispatcherDisconnectedException
import mihon.tts.provider.speechdispatcher.protocol.SsipEvent
import java.io.IOException

/**
 * Keeps one connection to Speech Dispatcher, opened on first use and again after it drops. A server that isn't running
 * is started first; keeping the connection open keeps it running, as screen readers and browsers do.
 */
internal class SpeechDispatcherService(
    private val address: SpeechDispatcherAddress,
    private val launcher: SpeechDispatcherLauncher,
    private val clientName: String,
    private val onEvent: (SsipEvent) -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private val mutex = Mutex()

    @Volatile
    private var session: SpeechDispatcherSession? = null

    suspend fun connect(): SpeechDispatcherConnecting {
        session?.takeIf { it.connection.isOpen }?.let { return SpeechDispatcherConnecting.Connected(it) }
        return mutex.withLock {
            session?.takeIf { it.connection.isOpen }?.let { return@withLock SpeechDispatcherConnecting.Connected(it) }
            open()
        }
    }

    suspend fun close() = mutex.withLock {
        session?.connection?.close()
        session = null
    }

    private suspend fun open(): SpeechDispatcherConnecting {
        val connection = try {
            openConnection()
        } catch (error: IOException) {
            when (val started = startServer(error)) {
                is ServerStart.Connected -> started.connection
                is ServerStart.Failed -> return SpeechDispatcherConnecting.Unavailable(started.problem)
            }
        }
        return configure(connection)
    }

    /** Starts the server for a socket nothing listens on, then connects to it. */
    private suspend fun startServer(connectionError: IOException): ServerStart {
        if (address !is SpeechDispatcherAddress.UnixSocket) {
            val problem = if (launcher.isInstalled()) {
                SpeechDispatcherProblem.Unreachable(connectionError.message)
            } else {
                SpeechDispatcherProblem.NotInstalled
            }
            return ServerStart.Failed(problem)
        }
        return when (val start = launcher.start(address.path)) {
            SpeechDispatcherStart.NotInstalled -> ServerStart.Failed(SpeechDispatcherProblem.NotInstalled)
            is SpeechDispatcherStart.Failed -> ServerStart.Failed(SpeechDispatcherProblem.CouldNotStart(start.detail))
            SpeechDispatcherStart.Started -> {
                var lastError = connectionError
                repeat(CONNECT_ATTEMPTS_AFTER_START) { attempt ->
                    try {
                        return ServerStart.Connected(openConnection())
                    } catch (error: IOException) {
                        lastError = error
                        if (attempt < CONNECT_ATTEMPTS_AFTER_START - 1) delay(CONNECT_RETRY_DELAY_MILLIS)
                    }
                }
                ServerStart.Failed(SpeechDispatcherProblem.Unreachable(lastError.message))
            }
        }
    }

    private suspend fun openConnection(): SpeechDispatcherConnection = SpeechDispatcherConnection.open(
        address = address,
        onEvent = onEvent,
        onClosed = ::closed,
    )

    /** Names the client, asks for speech events, and reads the defaults before any setting of Katari's changes them. */
    private suspend fun configure(connection: SpeechDispatcherConnection): SpeechDispatcherConnecting {
        return try {
            listOf(
                "SET self CLIENT_NAME $clientName",
                "SET self NOTIFICATION all on",
            ).forEach { command ->
                val reply = connection.command(command)
                if (!reply.isSuccess) {
                    connection.close()
                    return SpeechDispatcherConnecting.Unavailable(SpeechDispatcherProblem.Refused(reply.statusLine))
                }
            }
            val defaultModule = connection.command("GET OUTPUT_MODULE").takeIf { it.isSuccess }?.data?.firstOrNull()
            val defaultLanguage = connection.command("GET LANGUAGE").takeIf { it.isSuccess }?.data?.firstOrNull()
            val opened = SpeechDispatcherSession(connection, defaultModule, defaultLanguage)
            session = opened
            SpeechDispatcherConnecting.Connected(opened)
        } catch (error: CancellationException) {
            connection.close()
            throw error
        } catch (_: SpeechDispatcherDisconnectedException) {
            SpeechDispatcherConnecting.Unavailable(SpeechDispatcherProblem.StoppedResponding)
        }
    }

    private fun closed(connection: SpeechDispatcherConnection) {
        if (session?.connection === connection) {
            session = null
            onDisconnected()
        }
    }

    private companion object {
        const val CONNECT_ATTEMPTS_AFTER_START = 10
        const val CONNECT_RETRY_DELAY_MILLIS = 100L
    }
}

/** [defaultModule] and [defaultLanguage] are the server's own settings, which pick its voice when Katari picks none. */
internal data class SpeechDispatcherSession(
    val connection: SpeechDispatcherConnection,
    val defaultModule: String?,
    val defaultLanguage: String?,
)

private sealed interface ServerStart {
    data class Connected(val connection: SpeechDispatcherConnection) : ServerStart

    data class Failed(val problem: SpeechDispatcherProblem) : ServerStart
}

internal sealed interface SpeechDispatcherConnecting {
    data class Connected(val session: SpeechDispatcherSession) : SpeechDispatcherConnecting

    data class Unavailable(val problem: SpeechDispatcherProblem) : SpeechDispatcherConnecting
}
