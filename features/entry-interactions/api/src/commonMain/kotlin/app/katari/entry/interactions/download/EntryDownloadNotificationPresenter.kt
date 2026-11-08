package app.katari.entry.interactions.download

import app.katari.core.common.platform.PlatformFacility

/**
 * Shows the download queue's notifications the way the platform posts them: one for progress, replaced in place, one
 * while the queue is paused, and the errors and warnings of single downloads. Shared code decides what to show; the
 * platform decides how, and leaves titles out where the user hides notification content.
 */
@PlatformFacility
interface EntryDownloadNotificationPresenter {
    fun showProgress(content: EntryDownloadProgressNotification)

    fun showPaused()

    /** Takes away the progress and paused notifications, as the queue has emptied or stopped. */
    fun onComplete()

    fun showError(content: EntryDownloadErrorNotification)

    /**
     * @param timeoutMillis how long the warning shows; null for as long as the platform keeps one.
     * @param helpUrl the page that explains the warning, which opening it shows; null to open the download queue.
     */
    fun showWarning(reason: String, timeoutMillis: Long? = null, helpUrl: String? = null)
}

/**
 * @param destination the entry the download belongs to, which the notification's action opens.
 * @param text what is downloading and how far it got; null where there is nothing to add to [title].
 * @param hiddenTitle what shows in place of [title] where notification content is hidden.
 */
data class EntryDownloadProgressNotification(
    val destination: EntryDownloadEntryIdentity,
    val title: String,
    val text: String?,
    val maximum: Int,
    val current: Int,
    val indeterminate: Boolean = false,
    val hiddenTitle: String? = null,
)

/** @param destination the entry that failed, which the notification's action opens; null where none is known. */
data class EntryDownloadErrorNotification(
    val destination: EntryDownloadEntryIdentity?,
    val title: String,
    val message: String,
)
