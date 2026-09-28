package mihon.entry.interactions.download.notification

import android.app.Notification
import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.mockk
import mihon.entry.interactions.download.EntryDownloadEntryIdentity
import mihon.entry.interactions.download.EntryDownloadNotifications
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class AndroidEntryDownloadNotifierTest {

    @Test
    fun `hidden notification content does not expose entry or child titles`() {
        val publisher = RecordingPublisher()
        val notifier = AndroidEntryDownloadNotifier(
            context = RuntimeEnvironment.getApplication(),
            actions = mockk(relaxed = true),
            hideNotificationContent = { true },
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

        notifier.showProgress(
            EntryDownloadProgressNotification(
                destination = EntryDownloadEntryIdentity(7L, EntryType.BOOK, 42L),
                title = "Private entry",
                text = "Private child",
                maximum = 100,
                current = 25,
            ),
        )
        notifier.showError(
            EntryDownloadErrorNotification(
                destination = null,
                title = "Private entry",
                message = "Private failure",
            ),
        )

        val progress = publisher.notifications.getValue(EntryDownloadNotifications.ID_PROGRESS)
        assertEquals("Downloader", progress.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertNull(progress.extras.getCharSequence(Notification.EXTRA_TEXT))
        val error = publisher.notifications.getValue(EntryDownloadNotifications.ID_ERROR)
        assertEquals("Downloader", error.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals("Unknown error", error.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    private class RecordingPublisher : EntryDownloadNotificationPublisher {
        val notifications = mutableMapOf<Int, Notification>()

        override fun notify(id: Int, notification: Notification) {
            notifications[id] = notification
        }

        override fun cancel(id: Int) {
            notifications -= id
        }
    }
}
