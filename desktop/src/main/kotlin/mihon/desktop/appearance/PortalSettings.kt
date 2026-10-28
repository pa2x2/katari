package mihon.desktop.appearance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Linux: the settings the XDG desktop portal publishes under `org.freedesktop.appearance`, read with GLib's `gdbus`,
 * which desktops running the portal ship, and followed through the portal's `SettingChanged` signal. One
 * `gdbus monitor` serves every setting followed. Without gdbus, or with a portal that doesn't publish a setting, the
 * setting reads as null. A D-Bus connection of the app's own replaces gdbus with the desktop platform services.
 *
 * The first read of a setting blocks the caller, the first window to compose, so the window opens in the right look;
 * the portal answers at once, and gdbus gives up after [READ_TIMEOUT_SECONDS].
 */
class PortalSettings(scope: CoroutineScope) {
    private val followed = mutableMapOf<String, FollowedSetting>()

    init {
        // Started before the first read, so a change made while a read runs is seen and wins over it
        val monitor = startGdbus("monitor", "--session", "--dest", PORTAL_NAME, "--object-path", PORTAL_PATH)
        if (monitor != null) {
            Runtime.getRuntime().addShutdownHook(Thread(monitor::destroy))
            scope.launch(Dispatchers.IO) {
                monitor.inputStream.bufferedReader().useLines { lines ->
                    lines.mapNotNull { CHANGE_SIGNAL.find(it) }.forEach { change ->
                        val (key, value) = change.destructured
                        val setting = synchronized(followed) { followed[key] } ?: return@forEach
                        synchronized(setting) {
                            setting.changed = true
                            setting.value.value = value
                        }
                    }
                }
            }
        }
    }

    /**
     * The value of [key] in `org.freedesktop.appearance` as gdbus prints it, a GVariant such as `uint32 1` inside
     * whatever wraps it; null while the portal publishes none.
     */
    fun appearance(key: String): StateFlow<String?> {
        val setting = synchronized(followed) {
            followed[key]?.let { return it.value.asStateFlow() }
            FollowedSetting().also { followed[key] = it }
        }
        val read = read(key)
        synchronized(setting) {
            if (!setting.changed) setting.value.value = read
        }
        return setting.value.asStateFlow()
    }

    private fun read(key: String): String? {
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
            key,
        ) ?: return null
        val output = read.inputStream.bufferedReader().use { it.readText() }
        if (!read.waitFor(READ_TIMEOUT_SECONDS + 1L, TimeUnit.SECONDS)) read.destroy()
        // A setting the portal doesn't publish is an error, printed to the discarded error stream
        return output.trim().ifEmpty { null }.also { value ->
            if (value == null) logcat(LogPriority.DEBUG) { "The desktop portal publishes no $key" }
        }
    }

    private fun startGdbus(vararg arguments: String): Process? = try {
        ProcessBuilder("gdbus", *arguments)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (e: IOException) {
        logcat(LogPriority.DEBUG, e) { "Can't reach the desktop portal through gdbus" }
        null
    }

    private class FollowedSetting {
        val value = MutableStateFlow<String?>(null)

        /** Whether the portal has signalled a change, which is newer than the first read. */
        var changed = false
    }

    private companion object {
        const val PORTAL_NAME = "org.freedesktop.portal.Desktop"
        const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
        const val APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
        const val READ_TIMEOUT_SECONDS = 2

        // gdbus prints a change as `…SettingChanged ('org.freedesktop.appearance', 'color-scheme', <uint32 1>)`
        val CHANGE_SIGNAL = Regex("""SettingChanged \('${Regex.escape(APPEARANCE_NAMESPACE)}', '([^']+)', (.*)\)$""")
    }
}
