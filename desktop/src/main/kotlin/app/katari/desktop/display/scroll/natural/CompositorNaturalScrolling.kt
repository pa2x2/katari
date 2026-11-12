package app.katari.desktop.display.scroll.natural

import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException

/**
 * Asks the Wayland compositor whether its touchpads scroll naturally, each compositor its own way, the one running
 * found by the name it owns on the session bus. Only KWin and Mutter are asked: other compositors keep the setting in
 * their own configuration, with no common way to read it.
 */
internal object CompositorNaturalScrolling {
    /** Whether the touchpads scroll naturally; null where the compositor can't be asked or doesn't say. Blocks. */
    fun read(): Boolean? = try {
        DbusConnection.session()?.use { bus ->
            when {
                bus.owns(KWinNaturalScrolling.SERVICE) -> KWinNaturalScrolling.read(bus)
                bus.owns(GnomeNaturalScrolling.SERVICE) -> GnomeNaturalScrolling.read()
                else -> null
            }
        }
    } catch (e: DbusException) {
        logcat(LogPriority.WARN, e) { "Can't ask the compositor whether its touchpads scroll naturally" }
        null
    } catch (e: IOException) {
        logcat(LogPriority.WARN, e) { "Can't ask the compositor whether its touchpads scroll naturally" }
        null
    }

    private fun DbusConnection.owns(name: String): Boolean =
        call(BUS, BUS_PATH, BUS, "NameHasOwner", "s", name).first() == true

    private const val BUS = "org.freedesktop.DBus"
    private const val BUS_PATH = "/org/freedesktop/DBus"
}
