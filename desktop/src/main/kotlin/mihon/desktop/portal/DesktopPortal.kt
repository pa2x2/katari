package mihon.desktop.portal

import mihon.desktop.dbus.DbusConnection
import mihon.desktop.dbus.DbusException
import mihon.desktop.dbus.Variant
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/**
 * Linux: the XDG desktop portal on the session bus, the service whose interfaces (file chooser, settings, network
 * monitor, …) the desktop's own backends answer. Which interfaces it has depends on the backends installed: it
 * offers an interface only where one of them implements it.
 */
internal object DesktopPortal {
    const val SERVICE = "org.freedesktop.portal.Desktop"
    const val PATH = "/org/freedesktop/portal/desktop"

    // Long enough for the bus to start a portal that is installed but not running yet
    private val PROBE_TIMEOUT = 10.seconds

    /** Whether the portal offers [iface] on the session bus, and in which version. Blocks until the bus answers. */
    fun probe(iface: String): PortalInterface {
        val bus = DbusConnection.session() ?: return PortalInterface.Missing("there is no session bus")
        return bus.use {
            try {
                val version = bus.call(
                    SERVICE,
                    PATH,
                    "org.freedesktop.DBus.Properties",
                    "Get",
                    "ss",
                    iface,
                    "version",
                    timeout = PROBE_TIMEOUT,
                ).first() as Variant
                PortalInterface.Offered(version.value as Int)
            } catch (e: DbusException) {
                val reason = if (e.name in NO_SERVICE_ERRORS || e.name.startsWith(SPAWN_ERROR_PREFIX)) {
                    "no portal on the session bus"
                } else {
                    "the portal has no $iface"
                }
                PortalInterface.Missing("$reason (${e.name})")
            } catch (e: IOException) {
                PortalInterface.Missing("the portal didn't answer (${e.message})")
            }
        }
    }

    /**
     * Whether [error], the answer to a call of one of the portal's methods, says that the portal or the interface isn't
     * there, as when the portal has quit and none can be started, or a backend has gone, rather than that the call
     * failed.
     */
    fun isMissing(error: DbusException): Boolean =
        error.name in NO_SERVICE_ERRORS || error.name in NO_INTERFACE_ERRORS ||
            error.name.startsWith(SPAWN_ERROR_PREFIX)

    private val NO_SERVICE_ERRORS = setOf(
        "org.freedesktop.DBus.Error.ServiceUnknown",
        "org.freedesktop.DBus.Error.NameHasNoOwner",
    )

    // GDBus, which the portal is built on, answers a call of an interface it doesn't export with UnknownMethod
    private val NO_INTERFACE_ERRORS = setOf(
        "org.freedesktop.DBus.Error.UnknownMethod",
        "org.freedesktop.DBus.Error.UnknownInterface",
        "org.freedesktop.DBus.Error.UnknownObject",
    )

    // A portal the bus knows how to start that failed to start
    private const val SPAWN_ERROR_PREFIX = "org.freedesktop.DBus.Error.Spawn."
}

internal sealed interface PortalInterface {
    data class Offered(val version: Int) : PortalInterface

    /** [reason] says why, for the log. */
    data class Missing(val reason: String) : PortalInterface
}
