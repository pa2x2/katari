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
 * shade. Work under way stays while it runs, as Android's ongoing notifications do: it never expires and is resident,
 * so clicking it or one of its actions doesn't take it away. Only the user's dismissal ends its display until its key
 * is withdrawn, so updates of a long task don't bring back what the user dismissed; one the server closed on its own,
 * such as a server set to override the app's timeouts, shows again with the next update. Without a server, each
 * notification goes to [SystemNotifications.Listener.unshown], and that is logged once until a server shows one again.
 *
 * The ids of what shows are kept in the [record] as well, so a later connection or run of the app replaces or takes
 * away what an earlier one showed under a key, as Android's ids do. They are taken in when the server is first reached,
 * and only while the same server runs; the work under way an earlier run showed ended with it, so that is taken away.
 */
internal class FreedesktopNotifications(
    private val appName: String,
    /** Called on the notifications' thread. */
    private val appIcon: () -> String,
    private val connect: () -> NotificationServer?,
    private val record: ShownNotificationsRecord,
    private val listener: SystemNotifications.Listener,
) : SystemNotifications {
    private val commands = LinkedBlockingQueue<Command>()

    @Volatile
    private var server: NotificationServer? = null

    private val worker = lazy { thread(name = "Notifications", isDaemon = true, block = ::run) }

    // Read and written by the worker only
    private var capabilities: Set<String>? = null
    private var identity: String? = null
    private var tookInEarlierRun = false
    private val shown = HashMap<String, Shown>()
    private val keys = HashMap<Int, String>()
    private val activationTokens = HashMap<Int, String>()
    private val closedWork = HashSet<String>()
    private var toldNoService = false

    override fun show(key: String, notification: DesktopNotification) = send(Command.Show(key, notification))

    override fun withdraw(key: String) = send(Command.Withdraw(key))

    override fun resume() = send(Command.Resume)

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
            Command.Resume -> if (record.read()?.notifications.orEmpty().any { it.ongoing }) takeInRecorded()
            is Command.Close -> error("Closing ends the worker")
        }
    }

    private fun display(key: String, notification: DesktopNotification) {
        if (notification.isOngoing && key in closedWork) return
        val server = connected()
        if (server == null) {
            unshown(notification, "there is no session bus")
            return
        }
        try {
            val capabilities = knownCapabilities(server)
            val replaced = shown[key]
            val id = server.notify(notification.toServer(capabilities), replacesId = replaced?.id ?: 0)
            if (replaced != null && replaced.id != id) forget(replaced.id)
            shown[key] = Shown(id, notification.isOngoing, notification)
            keys[id] = key
            if (replaced?.id != id || replaced.ongoing != notification.isOngoing) save()
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
        // What an earlier run left under the key is known only once the server is reached
        if (key !in shown && capabilities == null && record.read()?.notifications.orEmpty().any { it.key == key }) {
            takeInRecorded()
        }
        val withdrawn = shown.remove(key) ?: return
        forget(withdrawn.id)
        save()
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
            shown.filterValues { it.ongoing }.forEach { (key, work) ->
                server?.closeNotification(work.id)
                shown -= key
            }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't take the notifications of work under way away" }
        } finally {
            // What couldn't be taken away stays recorded, for the next run to take away
            save()
            server?.close()
            command.closed.countDown()
        }
    }

    private fun connected(): NotificationServer? = server ?: connect()?.also { server = it }

    /** Reaches the server, which takes in the recorded notifications it shows. */
    private fun takeInRecorded() {
        try {
            connected()?.let(::knownCapabilities)
        } catch (_: NoNotificationService) {
            serverChanged()
        } catch (e: IOException) {
            disconnect(e)
        }
    }

    /** What [server] can show, read once each time it is reached, which takes in what [record] holds of it. */
    private fun knownCapabilities(server: NotificationServer): Set<String> {
        capabilities?.let { return it }
        val known = server.capabilities()
        val identity = server.identity()
        takeIn(server, identity)
        this.identity = identity
        capabilities = known
        save()
        return known
    }

    /**
     * Takes in the notifications [record] holds that the server of [identity] shows for the app. The first time in a
     * run they are an earlier run's, whose work under way ended with it and is taken away; after a lost connection,
     * they are this run's own.
     */
    private fun takeIn(server: NotificationServer, identity: String) {
        val recorded = record.read()
        if (recorded == null || recorded.server != identity) {
            record.clear()
            tookInEarlierRun = true
            return
        }
        val earlierRun = !tookInEarlierRun
        recorded.notifications.forEach { (key, id, ongoing) ->
            if (ongoing && earlierRun) {
                server.closeNotification(id)
            } else if (key !in shown) {
                shown[key] = Shown(id, ongoing, notification = null)
                keys[id] = key
            }
        }
        tookInEarlierRun = true
    }

    private fun save() {
        val identity = identity ?: return
        record.write(
            ShownNotifications(
                identity,
                shown.map { (key, entry) ->
                    ShownNotificationId(key, entry.id, entry.ongoing)
                },
            ),
        )
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
                val closed = shown.remove(key)
                save()
                if (closed?.ongoing == true && signal.reason == DISMISSED_BY_USER) closedWork += key
            }
            is NotificationSignal.ServerChanged -> {
                val sameServer = signal.identity != null && signal.identity == identity
                if (!sameServer) serverChanged()
            }
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
            isOngoing || PERSISTENCE !in capabilities -> NEVER_EXPIRES
            else -> SERVER_DEFAULT_TIMEOUT
        }
        return ServerNotification(
            appName = appName,
            appIcon = appIcon(),
            summary = summary,
            body = shownBody,
            actions = actions,
            urgency = when (urgency) {
                NotificationUrgency.Low -> URGENCY_LOW
                NotificationUrgency.Normal -> URGENCY_NORMAL
            },
            progressPercent = progress?.takeIf { it.total > 0 }?.let { it.done * 100 / it.total },
            expireTimeout = expireTimeout,
            resident = isOngoing,
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
        forgetServer()
        record.clear()
    }

    /** The server may still show what the app showed on it, which the [record] keeps for the next connection. */
    private fun disconnect(error: IOException) {
        logcat(LogPriority.WARN, error) { "Lost the connection to the notification server" }
        server?.close()
        server = null
        forgetServer()
    }

    private fun forgetServer() {
        capabilities = null
        identity = null
        shown.clear()
        keys.clear()
        activationTokens.clear()
        closedWork.clear()
    }

    /** @param notification null for one taken in from the [record], whose answers this run doesn't know. */
    private class Shown(val id: Int, val ongoing: Boolean, val notification: DesktopNotification?)

    private sealed interface Command {
        class Show(val key: String, val notification: DesktopNotification) : Command

        class Withdraw(val key: String) : Command

        data object Resume : Command

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
        const val DISMISSED_BY_USER = 2
        const val URGENCY_LOW: Byte = 0
        const val URGENCY_NORMAL: Byte = 1

        // A call ends the wait at once
        val IDLE_WAIT = 30.seconds
        val CLOSE_WAIT = 2.seconds
    }
}

/** The body markup servers read is a small subset of HTML, in which these three characters must be escaped. */
private fun String.escapedForMarkup(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
