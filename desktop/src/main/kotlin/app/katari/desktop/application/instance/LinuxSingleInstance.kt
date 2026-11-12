package app.katari.desktop.application.instance

import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.FileChannel
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Linux: the running instance holds a lock on a file in [directory] and answers later launches on a Unix domain socket
 * beside it ([LinuxRunningInstance]). The system drops a process's lock when the process ends, however it ends, so the
 * files a killed instance leaves block nothing: the next launch takes the lock and replaces the socket.
 *
 * A launch that finds the lock held sends its request on the socket. When nothing answers, the instance holding the
 * lock has not opened its socket yet or is ending, as on a restart, so the launch tries the lock and the socket again
 * until one of them succeeds or [HANDOVER_TIMEOUT] passes.
 */
internal class LinuxSingleInstance(
    private val directory: Path,
    private val environment: Map<String, String>,
) : SingleInstance {
    private val socket = UnixDomainSocketAddress.of(directory.resolve(SOCKET_FILE_NAME))

    override fun claim(links: List<String>, startsInTray: Boolean): InstanceClaim {
        val lockFile = FileChannel.open(
            directory.resolve(LOCK_FILE_NAME),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        )
        val request = InstanceRequest(
            activationToken = launchActivationToken(),
            links = links,
            startsInTray = startsInTray,
        )
        val deadline = System.nanoTime() + HANDOVER_TIMEOUT.inWholeNanoseconds
        while (true) {
            val lock = lockFile.tryLock()
            if (lock != null) return LinuxRunningInstance.start(lock, socket)
            if (handOver(request)) {
                lockFile.close()
                return InstanceClaim.HandedOver
            }
            if (System.nanoTime() >= deadline) {
                lockFile.close()
                return InstanceClaim.Unanswered
            }
            Thread.sleep(RETRY_INTERVAL.inWholeMilliseconds)
        }
    }

    /**
     * The token a launcher hands an app it starts, for the app to take the focus with: Wayland's, which launchers in a
     * Wayland session set, or else X11's startup id. Launchers that set both set the same token.
     */
    private fun launchActivationToken(): String? =
        environment["XDG_ACTIVATION_TOKEN"]?.ifEmpty { null } ?: environment["DESKTOP_STARTUP_ID"]?.ifEmpty { null }

    /** Whether the running instance took [request] within [ANSWER_TIMEOUT]. */
    private fun handOver(request: InstanceRequest): Boolean {
        val channel = try {
            SocketChannel.open(StandardProtocolFamily.UNIX).apply { connect(socket) }
        } catch (_: IOException) {
            return false
        }
        // Closing the channel ends a read that waits on an instance that doesn't answer
        val timeout = thread(isDaemon = true, name = "Instance answer timeout") {
            try {
                Thread.sleep(ANSWER_TIMEOUT.inWholeMilliseconds)
                channel.close()
            } catch (_: InterruptedException) {
                // Answered in time
            }
        }
        return try {
            channel.use {
                InstanceSocketMessages.write(channel, InstanceSocketMessages.encode(request))
                channel.shutdownOutput()
                InstanceSocketMessages.readAll(channel).decodeToString() == InstanceSocketMessages.ANSWER
            }
        } catch (_: IOException) {
            false
        } finally {
            timeout.interrupt()
        }
    }

    private companion object {
        const val LOCK_FILE_NAME = "instance.lock"

        // The AppImage's launcher looks for this file too, to show no splash on a launch the running instance answers
        // (AssembleAppDirTask in the build logic)
        const val SOCKET_FILE_NAME = "instance.socket"

        // Long enough for an instance to end, flushing its preferences, after it stopped answering for a restart
        val HANDOVER_TIMEOUT = 10.seconds
        val ANSWER_TIMEOUT = 2.seconds
        val RETRY_INTERVAL = 100.milliseconds
    }
}
