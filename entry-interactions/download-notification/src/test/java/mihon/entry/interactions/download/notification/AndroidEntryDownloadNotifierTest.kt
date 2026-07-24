package mihon.entry.interactions.download.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mihon.entry.interactions.EntryDownloadEntryIdentity
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryDownloadNotifications
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AndroidEntryDownloadNotifierTest {
    private lateinit var context: Context
    private lateinit var publisher: RecordingPublisher
    private lateinit var actions: EntryDownloadNotificationActions
    private var hideContent = false

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        publisher = RecordingPublisher()
        val pendingIntent = mockk<PendingIntent>(relaxed = true)
        actions = mockk {
            every { openDownloadManager(any()) } returns pendingIntent
            every { pauseDownloads(any()) } returns pendingIntent
            every { resumeDownloads(any()) } returns pendingIntent
            every { clearDownloads(any()) } returns pendingIntent
            every { openEntry(any(), any(), any()) } returns pendingIntent
            every { openUrl(any(), any()) } returns pendingIntent
        }
    }

    @Test
    fun `active notification is rebuilt after paused state`() {
        val notifier = notifier()

        notifier.showPaused()
        notifier.showProgress(progress())

        val notification = publisher.notifications.getValue(EntryDownloadNotifications.ID_PROGRESS)
        assertEquals(android.R.drawable.stat_sys_download, notification.icon)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(
            listOf("Pause", "Cancel all", "Show entry"),
            notification.actions.map { it.title.toString() },
        )
        verify { actions.openEntry(context, 7L, 42L) }
    }

    @Test
    fun `hidden notification content does not expose entry or child titles`() {
        hideContent = true
        val notifier = notifier()

        notifier.showProgress(progress())

        val notification = publisher.notifications.getValue(EntryDownloadNotifications.ID_PROGRESS)
        assertEquals(
            "Downloader",
            notification.extras.getCharSequence(Notification.EXTRA_TITLE),
        )
        assertNull(notification.extras.getCharSequence(Notification.EXTRA_TEXT))

        notifier.showError(
            EntryDownloadErrorNotification(
                destination = null,
                title = "Private entry",
                message = "Private failure",
            ),
        )
        val error = publisher.notifications.getValue(EntryDownloadNotifications.ID_ERROR)
        assertEquals("Downloader", error.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals("Unknown error", error.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun `paused and active states share one notification slot`() {
        val notifier = notifier()

        notifier.showPaused()
        assertEquals(setOf(EntryDownloadNotifications.ID_PROGRESS), publisher.notifications.keys)

        notifier.showProgress(progress())

        assertEquals(setOf(EntryDownloadNotifications.ID_PROGRESS), publisher.notifications.keys)
        assertFalse(publisher.notifications.containsKey(EntryDownloadNotifications.ID_ERROR))
    }

    private fun notifier() = AndroidEntryDownloadNotifier(
        context = context,
        actions = actions,
        hideNotificationContent = { hideContent },
        labels = EntryDownloadNotificationLabels(
            downloader = "Downloader",
            unknownError = "Unknown error",
            paused = "Paused",
            downloadsPaused = "Downloads paused",
            pause = "Pause",
            resume = "Resume",
            cancelAll = "Cancel all",
            showEntry = "Show entry",
        ),
        publisher = publisher,
    )

    private fun progress() = EntryDownloadProgressNotification(
        destination = EntryDownloadEntryIdentity(7L, EntryType.BOOK, 42L),
        title = "Private entry",
        text = "Private child",
        maximum = 100,
        current = 25,
    )

    private class RecordingPublisher : EntryDownloadNotificationPublisher {
        val notifications = mutableMapOf<Int, Notification>()
        val cancellations = mutableListOf<Int>()

        override fun notify(id: Int, notification: Notification) {
            notifications[id] = notification
        }

        override fun cancel(id: Int) {
            cancellations += id
            notifications -= id
        }
    }
}
