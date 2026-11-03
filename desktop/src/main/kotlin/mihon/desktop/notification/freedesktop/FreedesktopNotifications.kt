package mihon.desktop.notification.freedesktop

import logcat.LogPriority
import mihon.desktop.notification.DesktopNotification
import mihon.desktop.notification.NotificationUrgency
import mihon.desktop.notification.SystemNotifications
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

/**
 * Linux: notifications through the freedesktop notification server, the one GNOME, KDE, Xfce, Cinnamon, dunst, mako
 * and others provide. One thread owns the [NotificationServer], connected on the first notification: it carries out
 * the calls in order, remembers the server's id for each key, so a notification shown again under its key replaces
 * the one on screen, and between calls waits for the server's signals.
 *
 * What the server shows is read from its capabilities, never from which desktop runs: without `actions` nothing is
 * clickable, without `body` the body joins the title, `body-markup` gets the body escaped, and without `persistence`,
 * where a notification that expires is gone, results stay until the user dismisses them, as Android's stay in the
 * shade. Work under way that the user or the server closed isn't shown again until its key is withdrawn, so updates
 * of a long task don't bring back what the user dismissed. Without a server, each notification goes to
 * [SystemNotifications.Listener.unshown], and that is logged once until a server shows one again.
 */
internal class FreedesktopNotifications(
    private val appName: String,
    private val connect: () -> NotificationServer?,
    private val listener: SystemNotifications.Listener,
) : SystemNotifications {
    private val commands = LinkedBlockingQueue<Command>()

    @Volatile
    private var server: NotificationServer? = null

    private val worker = lazy { thread(name = "Notifications", isDaemon = true, block = ::run) }

    // Read and written by the worker only
    private var capabilities: Set<String>? = null
    private val shown = HashMap<String, Shown>()
    private val keys = HashMap<Int, String>()
    private val activationTokens = HashMap<Int, String>()
    private val closedWork = HashSet<String>()
    private var toldNoService = false

    override fun show(key: String, notification: DesktopNotification) = send(Command.Show(key, notification))

    override fun withdraw(key: String) = send(Command.Withdraw(key))

    override fun close() {
        if (!worker.isInitialized()) return
        val closed = CountDownLatch(1)
        send(Command.Close(closed))
        closed.await(CLOSE_WAIT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    private fun send(command: Command) {
        commands.put(command)
        worker.value
        server?.wakeUp()
    }

    private fun run() {
        while (true) {
            val server = server
            val command = if (server == null) commands.take() else commands.poll()
            when {
                command is Command.Close -> {
                    end(command)
                    return
                }
                command != null -> execute(command)
                else -> {
                    val signal = try {
                        server?.nextSignal(IDLE_WAIT)
                    } catch (e: IOException) {
                        disconnect(e)
                        null
                    }
                    signal?.let(::receive)
                }
            }
        }
    }

    private fun execute(command: Command) {
        when (command) {
            is Command.Show -> display(command.key, command.notification)
            is Command.Withdraw -> takeAway(command.key)
            is Command.Close -> error("Closing ends the worker")
        }
    }

    private fun display(key: String, notification: DesktopNotification) {
        if (notification.isOngoing && key in closedWork) return
        val server = server ?: connect()?.also { server = it }
        if (server == null) {
            unshown(notification, "there is no session bus")
            return
        }
        try {
            val capabilities = capabilities ?: server.capabilities().also { capabilities = it }
            val replaced = shown[key]
            val id = server.notify(notification.toServer(capabilities), replacesId = replaced?.id ?: 0)
            if (replaced != null && replaced.id != id) forget(replaced.id)
            shown[key] = Shown(id, notification)
            keys[id] = key
            toldNoService = false
        } catch (e: NoNotificationService) {
            serverChanged()
            unshown(notification, "no notification server is on the session bus (${e.message})")
        } catch (e: IOException) {
            disconnect(e)
            unshown(notification, "the notification server failed (${e.message})")
        }
    }

    private fun takeAway(key: String) {
        closedWork -= key
        val withdrawn = shown.remove(key) ?: return
        forget(withdrawn.id)
        try {
            server?.closeNotification(withdrawn.id)
        } catch (_: NoNotificationService) {
            serverChanged()
        } catch (e: IOException) {
            disconnect(e)
        }
    }

    private fun end(command: Command.Close) {
        val server = server
        try {
            shown.values.filter { it.notification.isOngoing }.forEach { server?.closeNotification(it.id) }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't take the notifications of work under way away" }
        } finally {
            server?.close()
            command.closed.countDown()
        }
    }

    private fun receive(signal: NotificationSignal) {
        when (signal) {
            is NotificationSignal.ActivationToken -> if (signal.id in keys) activationTokens[signal.id] = signal.token
            is NotificationSignal.ActionInvoked -> {
                val notification = keys[signal.id]?.let(shown::get)?.notification ?: return
                val response = if (signal.actionKey == DEFAULT_ACTION) {
                    notification.onClick
                } else {
                    signal.actionKey.toIntOrNull()?.let(notification.actions::getOrNull)?.response
                }
                val token = activationTokens.remove(signal.id)
                response?.let { listener.answered(it, token) }
            }
            is NotificationSignal.Closed -> {
                val key = keys[signal.id] ?: return
                forget(signal.id)
                if (shown.remove(key)?.notification?.isOngoing == true) closedWork += key
            }
            NotificationSignal.ServerChanged -> serverChanged()
        }
    }

    private fun DesktopNotification.toServer(capabilities: Set<String>): ServerNotification {
        val text = body?.let { if (BODY_MARKUP in capabilities) it.escapedForMarkup() else it }.orEmpty()
        val (summary, shownBody) = if (BODY in capabilities || text.isEmpty()) {
            title to text
        } else {
            "$title · ${body.orEmpty().lines().joinToString(" · ")}" to ""
        }
        val actions = if (ACTIONS in capabilities) {
            buildList {
                if (onClick != null) add(DEFAULT_ACTION to "")
                actions.forEachIndexed { index, action -> add(index.toString() to action.label) }
            }
        } else {
            emptyList()
        }
        val expireTimeout = when {
            timeout != null -> timeout.inWholeMilliseconds.toInt()
            !isOngoing && PERSISTENCE !in capabilities -> NEVER_EXPIRES
            else -> SERVER_DEFAULT_TIMEOUT
        }
        return ServerNotification(
            appName = appName,
            summary = summary,
            body = shownBody,
            actions = actions,
            urgency = when (urgency) {
                NotificationUrgency.Low -> URGENCY_LOW
                NotificationUrgency.Normal -> URGENCY_NORMAL
            },
            progressPercent = progress?.takeIf { it.total > 0 }?.let { it.done * 100 / it.total },
            expireTimeout = expireTimeout,
        )
    }

    private fun unshown(notification: DesktopNotification, reason: String) {
        if (!toldNoService) {
            logcat(LogPriority.INFO) { "Notifications show in the app instead: $reason" }
            toldNoService = true
        }
        listener.unshown(notification)
    }

    private fun forget(id: Int) {
        keys -= id
        activationTokens -= id
    }

    /** The notifications the old server showed went with it, and the new one may show different things. */
    private fun serverChanged() {
        capabilities = null
        shown.clear()
        keys.clear()
        activationTokens.clear()
        closedWork.clear()
    }

    private fun disconnect(error: IOException) {
        logcat(LogPriority.WARN, error) { "Lost the connection to the notification server" }
        server?.close()
        server = null
        serverChanged()
    }

    private class Shown(val id: Int, val notification: DesktopNotification)

    private sealed interface Command {
        class Show(val key: String, val notification: DesktopNotification) : Command

        class Withdraw(val key: String) : Command

        class Close(val closed: CountDownLatch) : Command
    }

    private companion object {
        const val DEFAULT_ACTION = "default"
        const val ACTIONS = "actions"
        const val BODY = "body"
        const val BODY_MARKUP = "body-markup"
        const val PERSISTENCE = "persistence"
        const val NEVER_EXPIRES = 0
        const val SERVER_DEFAULT_TIMEOUT = -1
        const val URGENCY_LOW: Byte = 0
        const val URGENCY_NORMAL: Byte = 1

        // A call ends the wait at once
        val IDLE_WAIT = 30.seconds
        val CLOSE_WAIT = 2.seconds
    }
}

/** The body markup servers read is a small subset of HTML, in which these three characters must be escaped. */
private fun String.escapedForMarkup(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
