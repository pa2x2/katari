package app.katari.desktop.portal

import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import app.katari.desktop.dbus.DbusMessage
import app.katari.desktop.dbus.Variant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

/**
 * Linux: a call of a portal method that answers through a request object (`org.freedesktop.portal.Request`), as the
 * file chooser's methods do. The method returns at once with the request's object path, and the portal sends the
 * request's `Response` signal when the user is done, which can be minutes later, to the connection that called. So
 * each request has a [DbusConnection] of its own, open until the answer; gdbus's connection per call would be gone.
 *
 * The request's path is known before the call, made of the connection's unique name and the `handle_token` the call
 * passes, and the match for its `Response` is added before the call, as the portal documentation asks: the portal may
 * answer before the call returns, as when its backend fails at once. Cancelling the coroutine closes the request,
 * which takes the portal's dialog away.
 */
internal object PortalRequest {
    private const val REQUEST_INTERFACE = "org.freedesktop.portal.Request"

    // How long one wait for a signal lasts before checking for cancellation again; the wait is woken at once anyway
    private val WAIT = 1.minutes

    private val tokens = AtomicInteger()

    /**
     * Calls [method] of the portal's [iface] with [arguments] of [signature], followed by [options] as the method's
     * last argument, an `a{sv}`, and returns the response. Throws [DbusException] when the portal answers the call with
     * an error, and [IOException] when the bus fails or the portal leaves the bus before responding.
     */
    suspend fun send(
        iface: String,
        method: String,
        signature: String,
        arguments: List<Any?>,
        options: Map<String, Variant>,
    ): PortalResponse = withContext(Dispatchers.IO) {
        val bus = DbusConnection.session() ?: throw IOException("There is no session bus to reach the portal on")
        bus.use {
            val token = "katari${tokens.incrementAndGet()}"
            val expected = requestHandlePath(bus.uniqueName, token)
            bus.addMatch(responseRule(expected))
            bus.addMatch(
                "type='signal',sender='${DbusConnection.BUS_NAME}',interface='${DbusConnection.BUS_NAME}'," +
                    "member='NameOwnerChanged',arg0='${DesktopPortal.SERVICE}'",
            )
            val handle = bus.call(
                DesktopPortal.SERVICE,
                DesktopPortal.PATH,
                iface,
                method,
                "${signature}a{sv}",
                *arguments.toTypedArray(),
                options + ("handle_token" to Variant("s", token)),
            ).first() as String
            // Portals before 0.9 made up a path of their own, which can only be matched once it is known
            if (handle != expected) bus.addMatch(responseRule(handle))
            val waker = launch {
                try {
                    awaitCancellation()
                } finally {
                    bus.wakeUp()
                }
            }
            try {
                awaitResponse(bus, handle)
            } finally {
                // Before the connection closes, which a wake-up mustn't come after
                withContext(NonCancellable) { waker.cancelAndJoin() }
            }
        }
    }

    private fun CoroutineScope.awaitResponse(bus: DbusConnection, handle: String): PortalResponse {
        while (true) {
            if (!isActive) {
                close(bus, handle)
                ensureActive()
            }
            val signal = bus.nextSignal(WAIT) ?: continue
            when {
                signal.isResponseOf(handle) -> return response(signal.body)
                signal.member == "NameOwnerChanged" && (signal.body.getOrNull(2) as? String).isNullOrEmpty() ->
                    throw IOException("The portal left the bus before responding")
            }
        }
    }

    private fun close(bus: DbusConnection, handle: String) {
        try {
            bus.call(DesktopPortal.SERVICE, handle, REQUEST_INTERFACE, "Close")
        } catch (e: IOException) {
            logcat(LogPriority.DEBUG, e) { "Couldn't close the portal request $handle" }
        } catch (e: DbusException) {
            // The request may have ended in the meantime, taking its object with it
            logcat(LogPriority.DEBUG, e) { "Couldn't close the portal request $handle" }
        }
    }

    private fun DbusMessage.isResponseOf(handle: String): Boolean =
        iface == REQUEST_INTERFACE && member == "Response" && path == handle

    private fun response(body: List<Any?>): PortalResponse = when (body[0] as Int) {
        0 -> PortalResponse.Done(
            (body[1] as Map<*, *>).entries.associate { (key, value) -> key as String to value as Variant },
        )
        1 -> PortalResponse.Cancelled
        else -> PortalResponse.Ended
    }

    private fun responseRule(path: String): String =
        "type='signal',sender='${DesktopPortal.SERVICE}',interface='$REQUEST_INTERFACE',member='Response',path='$path'"
}

/**
 * The object path the portal gives the request that the connection named [uniqueName] makes with [token]: the name
 * without its leading colon and with its dots made underscores, so `:1.42` makes `…/request/1_42/<token>`.
 */
internal fun requestHandlePath(uniqueName: String, token: String): String =
    "${DesktopPortal.PATH}/request/${uniqueName.removePrefix(":").replace('.', '_')}/$token"

internal sealed interface PortalResponse {
    /** The user did what the call asked; [results] are what the method's documentation lists, by name. */
    data class Done(val results: Map<String, Variant>) : PortalResponse

    data object Cancelled : PortalResponse

    /** The portal ended the interaction some other way, such as its backend failing; it says no more about why. */
    data object Ended : PortalResponse
}
