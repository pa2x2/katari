package mihon.desktop.portal

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Linux: the XDG desktop portal, reached with GLib's `gdbus`, which desktops running the portal ship. Without gdbus, a
 * call answers null and no signal arrives. A D-Bus connection of the app's own replaces gdbus with the desktop
 * platform services.
 */
internal object PortalGdbus {
    private const val PORTAL_NAME = "org.freedesktop.portal.Desktop"
    private const val PORTAL_PATH = "/org/freedesktop/portal/desktop"

    /**
     * Calls [method] of the portal with [arguments] and returns what gdbus prints for the answer, a GVariant tuple such
     * as `(uint32 1,)`; null when the portal doesn't answer, such as for a method it lacks, or after [timeoutSeconds].
     */
    fun call(method: String, vararg arguments: String, timeoutSeconds: Int): String? {
        val call = start(
            "call",
            "--session",
            "--timeout",
            timeoutSeconds.toString(),
            "--dest",
            PORTAL_NAME,
            "--object-path",
            PORTAL_PATH,
            "--method",
            method,
            *arguments,
        ) ?: return null
        val output = call.inputStream.bufferedReader().use { it.readText() }
        if (!call.waitFor(timeoutSeconds + 1L, TimeUnit.SECONDS)) call.destroy()
        // A method the portal lacks is an error, printed to the discarded error stream
        return output.trim().ifEmpty { null }
    }

    /** A gdbus process printing a line for each signal the portal sends; destroy it to stop. */
    fun monitor(): Process? = start("monitor", "--session", "--dest", PORTAL_NAME, "--object-path", PORTAL_PATH)

    private fun start(vararg arguments: String): Process? = try {
        ProcessBuilder("gdbus", *arguments)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (e: IOException) {
        logcat(LogPriority.DEBUG, e) { "Can't reach the desktop portal through gdbus" }
        null
    }
}
