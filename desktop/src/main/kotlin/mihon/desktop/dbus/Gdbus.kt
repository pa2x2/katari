package mihon.desktop.dbus

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Linux: one D-Bus object of a desktop service, reached with GLib's `gdbus`, which desktops ship. Without gdbus, a call
 * answers null and no signal arrives. A D-Bus connection of the app's own replaces gdbus with the desktop platform
 * services.
 */
internal open class Gdbus(private val bus: Bus, private val destination: String, private val objectPath: String) {
    enum class Bus(val option: String) {
        Session("--session"),
        System("--system"),
    }

    /**
     * Calls [method] of the object with [arguments] and returns what gdbus prints for the answer, a GVariant tuple such
     * as `(uint32 1,)`; null when the service doesn't answer, such as for a method it lacks, or after [timeoutSeconds].
     */
    fun call(method: String, vararg arguments: String, timeoutSeconds: Int): String? {
        val call = start(
            "call",
            bus.option,
            "--timeout",
            timeoutSeconds.toString(),
            "--dest",
            destination,
            "--object-path",
            objectPath,
            "--method",
            method,
            *arguments,
        ) ?: return null
        val output = call.inputStream.bufferedReader().use { it.readText() }
        if (!call.waitFor(timeoutSeconds + 1L, TimeUnit.SECONDS)) call.destroy()
        // A method the service lacks is an error, printed to the discarded error stream
        return output.trim().ifEmpty { null }
    }

    fun monitor(): Monitor? =
        start("monitor", bus.option, "--dest", destination, "--object-path", objectPath)?.let(::Monitor)

    private fun start(vararg arguments: String): Process? = try {
        ProcessBuilder("gdbus", *arguments)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (e: IOException) {
        logcat(LogPriority.DEBUG, e) { "Can't reach $destination through gdbus" }
        null
    }

    /**
     * A running gdbus monitor, printing a line for each signal the object sends, until [stop] or the app's exit. It is
     * killed through its process handle, not [Process.destroy], which closes the output under the reader blocked on it
     * and fails that read; the reader instead sees the output end.
     */
    class Monitor internal constructor(private val process: Process) {
        private val stopOnExit = Thread { process.toHandle().destroy() }

        init {
            Runtime.getRuntime().addShutdownHook(stopOnExit)
        }

        /** The signals' lines as they arrive, each read blocking until one does; they end once the monitor stops. */
        val signals: Sequence<String> = process.inputStream.bufferedReader().lineSequence()

        fun stop() {
            try {
                Runtime.getRuntime().removeShutdownHook(stopOnExit)
            } catch (_: IllegalStateException) {
                // The app is exiting, and the hook stops the monitor
            }
            process.toHandle().destroy()
        }
    }
}
