package mihon.desktop.appearance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.presentation.kit.theme.SystemColorScheme
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Linux: the `color-scheme` the XDG desktop portal publishes under `org.freedesktop.appearance`, read once and then
 * followed through the portal's `SettingChanged` signal with GLib's `gdbus`, which desktops running the portal ship.
 * Without gdbus, or with a portal that doesn't publish the setting, the desktop states no preference. A D-Bus
 * connection of the app's own replaces gdbus with the desktop platform services.
 *
 * The first read blocks the caller, the first window to compose, so the window opens in the right colours; the portal
 * answers at once, and gdbus gives up after [READ_TIMEOUT_SECONDS].
 */
class PortalColorScheme(scope: CoroutineScope) : SystemColorScheme {
    private val state = MutableStateFlow(false)

    override val prefersDark: StateFlow<Boolean> = state.asStateFlow()

    init {
        // Started before the first read, so a change made while it runs waits in the pipe and wins over it
        val monitor = startGdbus(
            "monitor",
            "--session",
            "--dest",
            PORTAL_NAME,
            "--object-path",
            PORTAL_PATH,
        )
        readColorScheme()?.let { state.value = it.prefersDark() }
        if (monitor != null) {
            Runtime.getRuntime().addShutdownHook(Thread(monitor::destroy))
            scope.launch(Dispatchers.IO) {
                monitor.inputStream.bufferedReader().useLines { lines ->
                    lines.filter { CHANGE_SIGNAL in it }
                        .mapNotNull(::colorSchemeValue)
                        .forEach { state.value = it.prefersDark() }
                }
            }
        }
    }

    private fun readColorScheme(): Int? {
        val read = startGdbus(
            "call",
            "--session",
            "--timeout",
            READ_TIMEOUT_SECONDS.toString(),
            "--dest",
            PORTAL_NAME,
            "--object-path",
            PORTAL_PATH,
            "--method",
            "org.freedesktop.portal.Settings.Read",
            APPEARANCE_NAMESPACE,
            COLOR_SCHEME_KEY,
        ) ?: return null
        val output = read.inputStream.bufferedReader().use { it.readText() }
        if (!read.waitFor(READ_TIMEOUT_SECONDS + 1L, TimeUnit.SECONDS)) read.destroy()
        return colorSchemeValue(output).also { value ->
            if (value == null) logcat(LogPriority.INFO) { "The desktop portal publishes no colour scheme" }
        }
    }

    private fun startGdbus(vararg arguments: String): Process? = try {
        ProcessBuilder("gdbus", *arguments)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (e: IOException) {
        logcat(LogPriority.INFO, e) { "Can't reach the desktop portal through gdbus" }
        null
    }

    // gdbus prints the value as a GVariant, `(<<uint32 1>>,)` for a read and `<uint32 1>` in a change signal
    private fun colorSchemeValue(output: String): Int? = UINT32_VALUE.find(output)?.groupValues?.get(1)?.toIntOrNull()

    // 1 is a preference for dark, 2 for light and 0 none
    private fun Int.prefersDark(): Boolean = this == 1

    private companion object {
        const val PORTAL_NAME = "org.freedesktop.portal.Desktop"
        const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
        const val APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
        const val COLOR_SCHEME_KEY = "color-scheme"
        const val CHANGE_SIGNAL = "SettingChanged ('$APPEARANCE_NAMESPACE', '$COLOR_SCHEME_KEY'"
        const val READ_TIMEOUT_SECONDS = 2
        val UINT32_VALUE = Regex("""uint32 (\d+)""")
    }
}
