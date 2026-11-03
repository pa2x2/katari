package mihon.desktop.application.instance

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileLock
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import kotlin.concurrent.thread

/**
 * The running instance of [LinuxSingleInstance]: it holds [lock] until the process ends and takes later launches'
 * requests on [server], each connection on a thread of its own, so a launch that never finishes its request holds no
 * other back.
 */
internal class LinuxRunningInstance private constructor(
    // Held, not read: the lock lasts as long as its channel stays open
    private val lock: FileLock,
    private val socket: UnixDomainSocketAddress,
    private val server: ServerSocketChannel?,
) : InstanceClaim.Running {
    private val received = Channel<InstanceRequest>(Channel.UNLIMITED)

    override val requests: Flow<InstanceRequest> = received.receiveAsFlow()

    override fun stopAnswering() {
        if (server == null) return
        try {
            server.close()
            Files.deleteIfExists(socket.path)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't close the instance socket" }
        }
    }

    private fun acceptRequests(server: ServerSocketChannel) {
        while (true) {
            val connection = try {
                server.accept()
            } catch (_: ClosedChannelException) {
                return
            } catch (e: IOException) {
                logcat(LogPriority.ERROR, e) { "The instance socket stopped taking launches" }
                return
            }
            thread(isDaemon = true, name = "Instance request") { answer(connection) }
        }
    }

    private fun answer(connection: SocketChannel) {
        try {
            connection.use {
                val request = InstanceSocketMessages.decode(InstanceSocketMessages.readAll(connection))
                if (request == null) {
                    logcat(LogPriority.WARN) { "A launch sent a request this version doesn't read" }
                    return
                }
                received.trySend(request)
                InstanceSocketMessages.write(connection, InstanceSocketMessages.ANSWER.encodeToByteArray())
            }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "A launch's request broke off" }
        }
    }

    companion object {
        /**
         * Starts answering on [socket], replacing the socket file an instance that was killed left behind; only the
         * holder of [lock] may. Without the socket the app still runs as the only instance, and later launches wait
         * for it to end.
         */
        fun start(lock: FileLock, socket: UnixDomainSocketAddress): LinuxRunningInstance {
            val server = try {
                Files.deleteIfExists(socket.path)
                ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(socket)
            } catch (e: IOException) {
                logcat(LogPriority.ERROR, e) { "Can't answer later launches on ${socket.path}" }
                null
            }
            return LinuxRunningInstance(lock, socket, server).also { instance ->
                if (server != null) {
                    thread(isDaemon = true, name = "Instance socket") { instance.acceptRequests(server) }
                }
            }
        }
    }
}
