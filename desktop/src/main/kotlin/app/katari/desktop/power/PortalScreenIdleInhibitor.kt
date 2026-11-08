package app.katari.desktop.power

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import app.katari.desktop.dbus.Variant
import app.katari.desktop.portal.DesktopPortal
import app.katari.desktop.portal.portalParentWindow
import app.katari.desktop.window.WindowPlatformContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Linux: the XDG desktop portal's `Inhibit` with the idle flag, which the desktop's own backend turns into its screen
 * saver's inhibition. The inhibition lasts as long as the request object the call returns, which the portal also
 * closes when the connection that asked leaves the bus, so each hold keeps a connection of its own until it is closed.
 * The calls run on a thread of their own, in order, so a hold closed at once is still taken and let go in turn.
 * Without the portal, or with a backend that can't inhibit, the screen locks as it would anyway.
 */
class PortalScreenIdleInhibitor : ScreenIdleInhibitor {
    private val calls = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Screen idle inhibitor").apply { isDaemon = true }
    }

    override fun keepAwake(window: PlatformContext, reason: String): AutoCloseable {
        val parent = portalParentWindow((window as? WindowPlatformContext)?.window)
        val hold = Hold()
        calls.execute { hold.take(parent, reason) }
        return AutoCloseable { calls.execute(hold::letGo) }
    }

    /** Confined to the calls' thread. */
    private class Hold {
        private var bus: DbusConnection? = null
        private var request: String? = null
        private var released = false

        fun take(parent: String, reason: String) {
            if (released) return
            val bus = DbusConnection.session() ?: return logcat(LogPriority.WARN) {
                "No session bus to keep the screen awake through"
            }
            try {
                request = bus.call(
                    DesktopPortal.SERVICE,
                    DesktopPortal.PATH,
                    INHIBIT_INTERFACE,
                    "Inhibit",
                    "sua{sv}",
                    parent,
                    IDLE,
                    mapOf(
                        "reason" to Variant("s", reason),
                        "handle_token" to Variant("s", "katari_idle${tokens.incrementAndGet()}"),
                    ),
                ).first() as String
                this.bus = bus
            } catch (e: DbusException) {
                bus.close()
                logcat(LogPriority.WARN) { "The portal didn't keep the screen awake: ${e.message}" }
            } catch (e: IOException) {
                bus.close()
                logcat(LogPriority.WARN, e) { "The portal didn't keep the screen awake" }
            }
        }

        fun letGo() {
            released = true
            val bus = bus ?: return
            try {
                request?.let { bus.call(DesktopPortal.SERVICE, it, REQUEST_INTERFACE, "Close") }
            } catch (e: DbusException) {
                logcat(LogPriority.DEBUG) { "Couldn't close the screen inhibition: ${e.message}" }
            } catch (e: IOException) {
                logcat(LogPriority.DEBUG, e) { "Couldn't close the screen inhibition" }
            } finally {
                // Leaving the bus ends the inhibition anyway
                bus.close()
                this.bus = null
            }
        }
    }

    private companion object {
        const val INHIBIT_INTERFACE = "org.freedesktop.portal.Inhibit"
        const val REQUEST_INTERFACE = "org.freedesktop.portal.Request"

        /** The portal's flag for keeping the session from going idle, which blanks and locks the screen. */
        const val IDLE = 8

        val tokens = AtomicInteger()
    }
}
