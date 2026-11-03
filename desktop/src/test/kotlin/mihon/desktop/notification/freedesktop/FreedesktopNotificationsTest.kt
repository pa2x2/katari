package mihon.desktop.notification.freedesktop

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mihon.desktop.notification.DesktopNotification
import mihon.desktop.notification.NotificationProgress
import mihon.desktop.notification.NotificationResponse
import mihon.desktop.notification.SystemNotifications
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration

class FreedesktopNotificationsTest {

    @Test
    fun `progress posted from several updates at once and withdrawn when they end leaves no notification behind`() {
        val server = FakeServer()
        val notifications = FreedesktopNotifications("Katari", { server }, Listener())

        notifications.show("result", DesktopNotification(title = "New chapters"))
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
        val notifications = FreedesktopNotifications("Katari", { server }, listener)

        notifications.show("updates", DesktopNotification(title = "New chapters"))
        listener.unshown.poll(WAIT, TimeUnit.SECONDS)?.title shouldBe "New chapters"

        server.start()
        notifications.show("updates", DesktopNotification(title = "More chapters"))
        notifications.close()

        listener.unshown.toList().shouldBeEmpty()
        server.open.values.map { it.summary } shouldContainExactly listOf("More chapters")
    }

    @Test
    fun `progress the server expired shows again with the next update, and progress the user dismissed doesn't`() {
        val server = FakeServer()
        val notifications = FreedesktopNotifications("Katari", { server }, Listener())

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

    private fun progress(done: Int) = DesktopNotification(title = "Progress", progress = NotificationProgress(done, 50))

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

        fun start() {
            running = true
            signals += NotificationSignal.ServerChanged
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
