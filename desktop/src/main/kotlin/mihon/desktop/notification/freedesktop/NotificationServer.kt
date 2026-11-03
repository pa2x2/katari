package mihon.desktop.notification.freedesktop

import java.io.Closeable
import java.io.IOException
import kotlin.time.Duration

/**
 * Linux: the server of the freedesktop notification specification on the session bus, `org.freedesktop.Notifications`,
 * as a connection of the app's own reaches it: the calls the app makes and the signals the server sends back to that
 * connection. Ids are the server's, unique while it runs. One thread at a time uses it, except for [wakeUp].
 * Throws [NoNotificationService] when no server owns the name and none can be started, and [IOException] when the
 * connection or the server fails.
 */
internal interface NotificationServer : Closeable {
    /** What the server can show, such as `actions`, `body-markup` and `persistence`. */
    fun capabilities(): Set<String>

    /** Shows [notification] in place of the one with [replacesId], or as a new one for 0, and returns its id. */
    fun notify(notification: ServerNotification, replacesId: Int): Int

    /** Takes [id] away; one that is gone already is no error. */
    fun closeNotification(id: Int)

    /**
     * Which server runs, unlike any other that ran or will run on any bus: ids are the server's, so an id is the app's
     * only on the server of the identity it had when the app was given it.
     */
    fun identity(): String

    /** The next signal, or null when none comes within [timeout] or [wakeUp] ends the wait. */
    fun nextSignal(timeout: Duration): NotificationSignal?

    fun wakeUp()
}

/**
 * The arguments of a `Notify` call.
 *
 * @param appIcon a `file://` URI of the icon, or the empty string for none.
 * @param actions pairs of an action's key and its label; the key `default` is what clicking the notification invokes.
 * @param progressPercent the `value` hint, which some servers show as a progress bar.
 * @param expireTimeout milliseconds; -1 for the server's default, 0 for never.
 * @param resident the `resident` hint: invoking an action doesn't take the notification away, only the user or the app
 * does.
 */
internal data class ServerNotification(
    val appName: String,
    val appIcon: String,
    val summary: String,
    val body: String,
    val actions: List<Pair<String, String>>,
    val urgency: Byte,
    val progressPercent: Int?,
    val expireTimeout: Int,
    val resident: Boolean,
)

internal sealed interface NotificationSignal {
    data class ActionInvoked(val id: Int, val actionKey: String) : NotificationSignal

    /** Sent before [ActionInvoked] by servers of version 1.2 and later. */
    data class ActivationToken(val id: Int, val token: String) : NotificationSignal

    /** [reason]: 1 expired, 2 dismissed by the user, 3 closed by `CloseNotification`, 4 undefined. */
    data class Closed(val id: Int, val reason: Int) : NotificationSignal

    /**
     * Another process owns the server's name now, or none does: the notifications the old server showed are gone.
     * [identity] is the new server's [NotificationServer.identity], or null where none runs or it isn't known; a server
     * started on demand by the app's first call sends this after that call, with the identity the app already has.
     */
    data class ServerChanged(val identity: String?) : NotificationSignal
}

internal class NoNotificationService(cause: Throwable) : IOException(cause.message, cause)
