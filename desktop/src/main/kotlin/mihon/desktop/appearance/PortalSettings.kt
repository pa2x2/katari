package mihon.desktop.appearance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.desktop.portal.PortalGdbus
import tachiyomi.core.common.util.system.logcat

/**
 * Linux: the settings the XDG desktop portal publishes, such as those under `org.freedesktop.appearance`, read through
 * [PortalGdbus] and followed through the portal's `SettingChanged` signal. One monitor serves every setting followed.
 * Without the portal, or with one that doesn't publish a setting, the setting reads as null.
 *
 * The first read of a setting blocks the caller, the first window to compose, so the window opens in the right look;
 * the portal answers at once, and the read gives up after [READ_TIMEOUT_SECONDS].
 */
class PortalSettings(scope: CoroutineScope) {
    private val followed = mutableMapOf<String, FollowedSetting>()

    init {
        // Started before the first read, so a change made while a read runs is seen and wins over it
        val monitor = PortalGdbus.monitor()
        if (monitor != null) {
            Runtime.getRuntime().addShutdownHook(Thread(monitor::destroy))
            scope.launch(Dispatchers.IO) {
                monitor.inputStream.bufferedReader().useLines { lines ->
                    lines.mapNotNull { CHANGE_SIGNAL.find(it) }.forEach { change ->
                        val (namespace, key, value) = change.destructured
                        val setting = synchronized(followed) { followed[followedKey(namespace, key)] } ?: return@forEach
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
    fun appearance(key: String): StateFlow<String?> = setting(APPEARANCE_NAMESPACE, key)

    /** The value of [key] in [namespace], as [appearance] reads it. */
    fun setting(namespace: String, key: String): StateFlow<String?> {
        val setting = synchronized(followed) {
            followed[followedKey(namespace, key)]?.let { return it.value.asStateFlow() }
            FollowedSetting().also { followed[followedKey(namespace, key)] = it }
        }
        val read = read(namespace, key)
        synchronized(setting) {
            if (!setting.changed) setting.value.value = read
        }
        return setting.value.asStateFlow()
    }

    private fun read(namespace: String, key: String): String? {
        return PortalGdbus.call(
            "org.freedesktop.portal.Settings.Read",
            namespace,
            key,
            timeoutSeconds = READ_TIMEOUT_SECONDS,
        ).also { value ->
            if (value == null) logcat(LogPriority.DEBUG) { "The desktop portal publishes no $namespace $key" }
        }
    }

    private fun followedKey(namespace: String, key: String) = "$namespace $key"

    private class FollowedSetting {
        val value = MutableStateFlow<String?>(null)

        /** Whether the portal has signalled a change, which is newer than the first read. */
        var changed = false
    }

    private companion object {
        const val APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
        const val READ_TIMEOUT_SECONDS = 2

        // gdbus prints a change as `…SettingChanged ('org.freedesktop.appearance', 'color-scheme', <uint32 1>)`
        val CHANGE_SIGNAL = Regex("""SettingChanged \('([^']+)', '([^']+)', (.*)\)$""")
    }
}
