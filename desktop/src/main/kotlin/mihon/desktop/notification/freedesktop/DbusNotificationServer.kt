package mihon.desktop.notification.freedesktop

import mihon.desktop.dbus.DbusConnection
import mihon.desktop.dbus.DbusException
import mihon.desktop.dbus.Variant
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Linux: the notification server over a [DbusConnection] of the app's own, which the server's signals for the app's
 * notifications are sent to; a connection made per call, such as gdbus's, would be gone before they arrive.
 */
internal class DbusNotificationServer private constructor(private val bus: DbusConnection) : NotificationServer {
    override fun capabilities(): Set<String> =
        (call("GetCapabilities").first() as List<*>).mapTo(HashSet()) { it as String }

    override fun notify(notification: ServerNotification, replacesId: Int): Int {
        val hints = buildMap {
            put("urgency", Variant("y", notification.urgency))
            notification.progressPercent?.let { put("value", Variant("i", it)) }
            if (notification.resident) put("resident", Variant("b", true))
        }
        return call(
            "Notify",
            "susssasa{sv}i",
            notification.appName,
            replacesId,
            // The app has no installed icon yet; the server shows its own or the one of the app's desktop entry
            "",
            notification.summary,
            notification.body,
            notification.actions.flatMap { (key, label) -> listOf(key, label) },
            hints,
            notification.expireTimeout,
        ).first() as Int
    }

    override fun closeNotification(id: Int) {
        call("CloseNotification", "u", id)
    }

    override fun nextSignal(timeout: Duration): NotificationSignal? {
        val signal = bus.nextSignal(timeout) ?: return null
        val body = signal.body
        return when {
            signal.iface == DbusConnection.BUS_NAME && signal.member == "NameOwnerChanged" ->
                NotificationSignal.ServerChanged.takeIf { body.firstOrNull() == SERVICE }
            signal.iface != SERVICE -> null
            signal.member == "ActionInvoked" -> NotificationSignal.ActionInvoked(body[0] as Int, body[1] as String)
            signal.member == "ActivationToken" -> NotificationSignal.ActivationToken(body[0] as Int, body[1] as String)
            signal.member == "NotificationClosed" -> NotificationSignal.Closed(body[0] as Int, body[1] as Int)
            else -> null
        }
    }

    override fun wakeUp() = bus.wakeUp()

    override fun close() = bus.close()

    private fun call(member: String, signature: String = "", vararg arguments: Any?): List<Any?> = try {
        bus.call(SERVICE, PATH, SERVICE, member, signature, *arguments, timeout = CALL_TIMEOUT)
    } catch (e: DbusException) {
        if (e.name in NO_SERVICE_ERRORS || e.name.startsWith(SPAWN_ERROR_PREFIX)) throw NoNotificationService(e)
        throw IOException("The notification server answered $member with an error", e)
    }

    companion object {
        private const val SERVICE = "org.freedesktop.Notifications"
        private const val PATH = "/org/freedesktop/Notifications"

        // Long enough for the bus to start a server that is installed but not running, as dunst and mako are
        private val CALL_TIMEOUT = 10.seconds

        private val NO_SERVICE_ERRORS = setOf(
            "org.freedesktop.DBus.Error.ServiceUnknown",
            "org.freedesktop.DBus.Error.NameHasNoOwner",
        )

        // A server the bus knows how to start that failed to start, such as one that needs a display it can't find
        private const val SPAWN_ERROR_PREFIX = "org.freedesktop.DBus.Error.Spawn."

        /** A connection to the session bus that gets the server's signals, or null where there is no session bus. */
        fun connect(): DbusNotificationServer? {
            val bus = DbusConnection.session() ?: return null
            return try {
                // Servers send their signals to the connection that showed the notification, though some broadcast
                bus.addMatch("type='signal',sender='$SERVICE',interface='$SERVICE',path='$PATH'")
                bus.addMatch(
                    "type='signal',sender='${DbusConnection.BUS_NAME}',interface='${DbusConnection.BUS_NAME}'," +
                        "member='NameOwnerChanged',arg0='$SERVICE'",
                )
                DbusNotificationServer(bus)
            } catch (_: IOException) {
                bus.close()
                null
            } catch (_: DbusException) {
                bus.close()
                null
            }
        }
    }
}
