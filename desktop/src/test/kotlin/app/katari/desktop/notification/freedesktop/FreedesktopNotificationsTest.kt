package app.katari.desktop.notification.freedesktop

import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.NotificationProgress
import app.katari.desktop.notification.NotificationResponse
import app.katari.desktop.notification.SystemNotifications
import app.katari.notification.AppNotificationKind
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration

class FreedesktopNotificationsTest {
    @TempDir
    lateinit var runtimeDirectory: Path

    private val record by lazy { ShownNotificationsRecord(runtimeDirectory.resolve("notifications.json")) }

    @Test
    fun `progress posted from several updates at once and withdrawn when they end leaves no notification behind`() {
        val server = FakeServer()
        val notifications = notifications(server, Listener())

        notifications.show("result", DesktopNotification(kind = AppNotificationKind.COMMON, title = "New chapters"))
        val updates = (1..4).map {
            thread {
                repeat(50) { done -> notifications.show("progress", progress(done)) }
            }
        }
        updates.forEach(Thread::join)
        notifications.withdraw("progress")
        notifications.show("progress-of-the-next-update", progress(0))
        notifications.close()

        // One notification for each key, replaced in place; the work under way goes with the app, the result stays
        server.created.map { it.summary } shouldContainExactly listOf("New chapters", "Progress", "Progress")
        server.open.values.map { it.summary } shouldContainExactly listOf("New chapters")
    }

    @Test
    fun `without a notification server notifications reach the app, and a server started later shows them`() {
        val server = FakeServer(running = false)
        val listener = Listener()
        val notifications = notifications(server, listener)

        notifications.show("updates", DesktopNotification(kind = AppNotificationKind.COMMON, title = "New chapters"))
        listener.unshown.poll(WAIT, TimeUnit.SECONDS)?.title shouldBe "New chapters"

        server.start()
        notifications.show("updates", DesktopNotification(kind = AppNotificationKind.COMMON, title = "More chapters"))
        notifications.close()

        listener.unshown.toList().shouldBeEmpty()
        server.open.values.map { it.summary } shouldContainExactly listOf("More chapters")
    }

    @Test
    fun `progress the server expired shows again with the next update, and progress the user dismissed doesn't`() {
        val server = FakeServer()
        val notifications = notifications(server, Listener())

        notifications.show("progress", progress(1))
        server.closeOnItsOwn(server.notified.poll(WAIT, TimeUnit.SECONDS).shouldNotBeNull(), reason = 1)
        notifications.show("progress", progress(2))
        server.closeOnItsOwn(server.notified.poll(WAIT, TimeUnit.SECONDS).shouldNotBeNull(), reason = 2)
        notifications.show("progress", progress(3))
        notifications.withdraw("progress")
        notifications.show("progress", progress(0))
        notifications.close()

        server.created.map { it.progressPercent } shouldContainExactly listOf(2, 4, 0)
    }

    @Test
    fun `a later run replaces the result an earlier one left and takes away the work under way a crashed one left`() {
        val server = FakeServer()
        notifications(server, Listener()).apply {
            show("updates", DesktopNotification(kind = AppNotificationKind.COMMON, title = "2 updates"))
            close()
        }
        // A run that crashed while work was under way
        val crashedWork = server.notify(serverNotification("Progress"), replacesId = 0)
        record.write(
            ShownNotifications(
                server.identity(),
                record.read().shouldNotBeNull().notifications + ShownNotificationId("progress", crashedWork, true),
            ),
        )

        notifications(server, Listener()).apply {
            resume()
            show("updates", DesktopNotification(kind = AppNotificationKind.COMMON, title = "3 updates"))
            close()
        }

        server.open.values.map { it.summary } shouldContainExactly listOf("3 updates")
    }

    @Test
    fun `after the server restarted, a later run posts anew instead of replacing what took over its old id`() {
        val server = FakeServer()
        notifications(server, Listener()).apply {
            show("updates", DesktopNotification(kind = AppNotificationKind.COMMON, title = "2 updates"))
            close()
        }
        server.restart()
        server.notify(serverNotification("Another app's"), replacesId = 0)

        notifications(server, Listener()).apply {
            show("updates", DesktopNotification(kind = AppNotificationKind.COMMON, title = "3 updates"))
            close()
        }

        server.open.values.map { it.summary } shouldContainExactly listOf("Another app's", "3 updates")
    }

    private fun notifications(server: FakeServer, listener: Listener) =
        FreedesktopNotifications("Katari", { "" }, { server }, record, listener)

    private fun serverNotification(summary: String) = ServerNotification(
        appName = "",
        appIcon = "",
        summary = summary,
        body = "",
        actions = emptyList(),
        urgency = 1,
        progressPercent = null,
        expireTimeout = 0,
        resident = false,
    )

    private fun progress(done: Int) = DesktopNotification(
        kind = AppNotificationKind.COMMON,
        title = "Progress",
        progress = NotificationProgress(done, 50),
    )

    private class Listener : SystemNotifications.Listener {
        val unshown = LinkedBlockingQueue<DesktopNotification>()

        override fun answered(response: NotificationResponse, activationToken: String?) = Unit

        override fun unshown(notification: DesktopNotification) {
            unshown += notification
        }
    }

    /**
     * Answers as a server does: ids are its own, a notification replaces the one of `replacesId` only while that one
     * is open, and closing one sends `NotificationClosed`. Until [start], no server owns the name.
     */
    private class FakeServer(@Volatile private var running: Boolean = true) : NotificationServer {
        private val signals = LinkedBlockingQueue<Any>()
        private val handled = LinkedBlockingQueue<NotificationSignal>()
        private var taken: NotificationSignal? = null
        private var nextId = 1
        private var instance = 1
        val open = LinkedHashMap<Int, ServerNotification>()
        val created = mutableListOf<ServerNotification>()
        val notified = LinkedBlockingQueue<Int>()

        /** Closes [id] as the user or the server would, and returns once the app has taken the signal in. */
        fun closeOnItsOwn(id: Int, reason: Int) {
            val closed = NotificationSignal.Closed(id, reason)
            synchronized(this) { open.remove(id) }
            signals += closed
            handled.poll(WAIT, TimeUnit.SECONDS) shouldBe closed
        }

        /** Ends and starts again, as a restarted server does: with nothing open, and counting its ids from the start. */
        @Synchronized
        fun restart() {
            open.clear()
            nextId = 1
            instance++
        }

        fun start() {
            running = true
            signals += NotificationSignal.ServerChanged(identity())
        }

        override fun capabilities(): Set<String> {
            ensureRunning()
            return setOf("actions", "body", "body-markup", "persistence")
        }

        @Synchronized
        override fun notify(notification: ServerNotification, replacesId: Int): Int {
            ensureRunning()
            val id = replacesId.takeIf { it in open } ?: nextId++.also { created += notification }
            open[id] = notification
            notified += id
            return id
        }

        @Synchronized
        override fun identity() = "bus :1.$instance"

        @Synchronized
        override fun closeNotification(id: Int) {
            ensureRunning()
            if (open.remove(id) != null) signals += NotificationSignal.Closed(id, reason = 3)
        }

        // The worker asks for the next signal only once it has handled the one before
        override fun nextSignal(timeout: Duration): NotificationSignal? {
            taken?.let { handled += it }
            return (signals.poll(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) as? NotificationSignal)
                .also { taken = it }
        }

        override fun wakeUp() {
            signals += WAKE_UP
        }

        override fun close() = Unit

        private fun ensureRunning() {
            if (!running) {
                throw NoNotificationService(
                    IllegalStateException("org.freedesktop.DBus.Error.ServiceUnknown"),
                )
            }
        }
    }

    private companion object {
        const val WAIT = 5L
        val WAKE_UP = Any()
    }
}
