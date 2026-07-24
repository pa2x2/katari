package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import mihon.entry.interactions.EntryLibraryUpdateNotificationAction
import mihon.entry.interactions.EntryLibraryUpdateNotificationDestination
import mihon.entry.interactions.EntryLibraryUpdateNotificationFeature
import mihon.entry.interactions.EntryLibraryUpdateNotificationGroup
import mihon.entry.interactions.EntryLibraryUpdateNotificationInput
import mihon.entry.interactions.EntryLibraryUpdateNotificationItem
import mihon.entry.interactions.EntryLibraryUpdateNotificationRoute
import mihon.entry.interactions.EntryLibraryUpdateNotificationText
import mihon.entry.interactions.EntryLibraryUpdateQueueWarning
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.math.RoundingMode
import java.text.NumberFormat

class LibraryUpdateNotifier(
    private val context: Context,
    private val securityPreferences: SecurityPreferences = Injekt.get(),
    private val notificationFeature: EntryLibraryUpdateNotificationFeature = Injekt.get(),
) {

    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelLibraryUpdatePendingBroadcast(context)
    }

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_LIBRARY_PROGRESS) {
            setContentTitle(context.stringResource(MR.strings.app_name))
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close_24dp, context.stringResource(MR.strings.action_cancel), cancelIntent)
        }
    }

    /**
     * Shows the notification containing the currently updating entries and the progress.
     *
     * @param entries the entries that are being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(entries: List<Entry>, current: Int, total: Int) {
        progressNotificationBuilder
            .setContentTitle(
                context.stringResource(
                    MR.strings.notification_updating_progress,
                    percentFormatter.format(current.toFloat() / total),
                ),
            )

        if (!securityPreferences.hideNotificationContent.get()) {
            val updatingText = entries.joinToString("\n") { it.title.chop(40) }
            progressNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(updatingText))
        }

        context.notify(
            Notifications.ID_LIBRARY_PROGRESS,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build(),
        )
    }

    /**
     * Warn when excessively checking any single source.
     */
    fun showQueueSizeWarningNotificationIfNeeded(entriesToUpdate: List<LibraryItem>) {
        if (
            notificationFeature.queueWarning(entriesToUpdate.map(LibraryItem::entry)) ==
            EntryLibraryUpdateQueueWarning.NotRequired
        ) {
            return
        }

        context.notify(
            Notifications.ID_LIBRARY_SIZE_WARNING,
            Notifications.CHANNEL_LIBRARY_PROGRESS,
        ) {
            setContentTitle(context.stringResource(MR.strings.label_warning))
            setStyle(
                NotificationCompat.BigTextStyle().bigText(context.stringResource(MR.strings.notification_size_warning)),
            )
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setTimeoutAfter(WARNING_NOTIF_TIMEOUT_MS)
            setContentIntent(NotificationHandler.openUrl(context, HELP_WARNING_URL))
        }
    }

    /**
     * Shows notification containing update entries that failed with action to open full log.
     *
     * @param failed Number of entries that failed to update.
     * @param uri Uri for error log file containing all titles that failed.
     */
    fun showUpdateErrorNotification(failed: Int, uri: Uri) {
        if (failed == 0) {
            return
        }

        context.notify(
            Notifications.ID_LIBRARY_ERROR,
            Notifications.CHANNEL_LIBRARY_ERROR,
        ) {
            setContentTitle(context.pluralStringResource(MR.plurals.notification_update_error, failed, failed))
            setContentText(context.stringResource(MR.strings.action_show_errors))
            setSmallIcon(R.drawable.ic_katari)

            setContentIntent(NotificationReceiver.openErrorLogPendingActivity(context, uri))
        }
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of entries with new updates.
     */
    fun showUpdateNotifications(updates: List<Pair<Entry, Array<EntryChapter>>>) {
        val projection = runBlocking {
            notificationFeature.project(
                updates.map { (entry, children) ->
                    EntryLibraryUpdateNotificationInput(
                        entry = entry,
                        children = children.toList(),
                    )
                },
            )
        }
        projection.omissions.forEach { omission ->
            logcat {
                "Library-update notifications omitted ${omission.updateCount} ${omission.type} updates: " +
                    omission.reason
            }
        }
        projection.groups.forEach(::showUpdateNotifications)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showUpdateNotifications(
        group: EntryLibraryUpdateNotificationGroup,
    ) {
        val route = group.route
        val childUpdates = group.updates
        // Parent group notification
        context.notify(
            route.summaryNotificationId,
            route.channelId,
        ) {
            setContentTitle(context.stringResource(group.summaryTitle))
            if (childUpdates.size == 1 && !securityPreferences.hideNotificationContent.get()) {
                setContentText(childUpdates.first().originEntry.displayTitle.chop(NOTIF_TITLE_MAX_LEN))
            } else {
                setContentText(
                    context.pluralStringResource(group.summaryText, childUpdates.size, childUpdates.size),
                )

                if (!securityPreferences.hideNotificationContent.get()) {
                    setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            childUpdates.joinToString("\n") {
                                it.originEntry.displayTitle.chop(NOTIF_TITLE_MAX_LEN)
                            },
                        ),
                    )
                }
            }

            setSmallIcon(R.drawable.ic_katari)
            setLargeIcon(notificationBitmap)

            setGroup(route.groupKey)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            setGroupSummary(true)
            priority = NotificationCompat.PRIORITY_HIGH

            setContentIntent(getNotificationIntent())
            setAutoCancel(true)
        }

        // Per-entry notification
        if (!securityPreferences.hideNotificationContent.get()) {
            launchUI {
                context.notify(
                    childUpdates.map { update ->
                        NotificationManagerCompat.NotificationWithIdAndTag(
                            update.originEntry.id.hashCode(),
                            createNewEntryNotification(
                                update = update,
                                route = route,
                            ),
                        )
                    },
                )
            }
        }
    }

    private suspend fun createNewEntryNotification(
        update: EntryLibraryUpdateNotificationItem,
        route: EntryLibraryUpdateNotificationRoute,
    ): Notification {
        val icon = getEntryIcon(update.originEntry)
        return context.notificationBuilder(route.channelId) {
            setContentTitle(update.originEntry.displayTitle)

            val description = context.render(update.description)
            setContentText(description)
            setStyle(NotificationCompat.BigTextStyle().bigText(description))

            setSmallIcon(R.drawable.ic_katari)

            if (icon != null) {
                setLargeIcon(icon)
            }

            setGroup(route.groupKey)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            priority = NotificationCompat.PRIORITY_HIGH

            setContentIntent(update.destinationIntent(context, route.summaryNotificationId))
            setAutoCancel(true)

            if (EntryLibraryUpdateNotificationAction.MARK_CONSUMED in update.actions) {
                addAction(
                    R.drawable.ic_done_24dp,
                    context.stringResource(update.markConsumedLabel),
                    update.markConsumedIntent(context, route.summaryNotificationId),
                )
            }
            if (EntryLibraryUpdateNotificationAction.VIEW_ENTRY in update.actions) {
                addAction(
                    R.drawable.ic_book_24dp,
                    context.stringResource(update.viewChildrenLabel),
                    update.viewEntryIntent(context, route.summaryNotificationId),
                )
            }
            if (EntryLibraryUpdateNotificationAction.DOWNLOAD in update.actions) {
                addAction(
                    android.R.drawable.stat_sys_download_done,
                    context.stringResource(MR.strings.action_download),
                    update.downloadChildrenIntent(context, route.summaryNotificationId),
                )
            }
        }.build()
    }

    /**
     * Cancels the progress notification.
     */
    fun cancelProgressNotification() {
        context.cancelNotification(Notifications.ID_LIBRARY_PROGRESS)
    }

    private suspend fun getEntryIcon(entry: Entry): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(entry)
            .transformations(CircleCropTransformation())
            .size(NOTIF_ICON_SIZE)
            .build()
        val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources)
        return drawable?.getBitmapOrNull()
    }

    /**
     * Returns an intent to open the main activity.
     */
    private fun getNotificationIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Constants.SHORTCUT_UPDATES
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val HELP_WARNING_URL =
            "https://mihon.app/docs/faq/library#why-am-i-warned-about-large-bulk-updates-and-downloads"
    }
}

private fun Context.render(text: EntryLibraryUpdateNotificationText): String = when (text) {
    is EntryLibraryUpdateNotificationText.StringText -> stringResource(
        text.resource,
        *text.arguments.toTypedArray(),
    )
    is EntryLibraryUpdateNotificationText.PluralText -> pluralStringResource(
        text.resource,
        text.quantity,
        *text.arguments.toTypedArray(),
    )
}

private fun EntryLibraryUpdateNotificationItem.destinationIntent(
    context: Context,
    summaryNotificationId: Int,
): PendingIntent = when (destination) {
    EntryLibraryUpdateNotificationDestination.OPEN_CHILD -> checkNotNull(
        NotificationReceiver.openChildPendingActivity(context, visibleEntry, originEntry, children.first()),
    ) { "Library-update notifications selected a child destination that Open could not render" }
    EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS -> NotificationReceiver.openEntryPendingActivity(
        context,
        visibleEntry.profileId,
        visibleEntry.id,
        summaryNotificationId,
    )
}

private fun EntryLibraryUpdateNotificationItem.markConsumedIntent(
    context: Context,
    summaryNotificationId: Int,
): PendingIntent = NotificationReceiver.markConsumedPendingBroadcast(
    context,
    originEntry,
    children.toTypedArray(),
    summaryNotificationId,
)

private fun EntryLibraryUpdateNotificationItem.viewEntryIntent(
    context: Context,
    summaryNotificationId: Int,
): PendingIntent = NotificationReceiver.openEntryPendingActivity(
    context,
    visibleEntry.profileId,
    visibleEntry.id,
    summaryNotificationId,
)

private fun EntryLibraryUpdateNotificationItem.downloadChildrenIntent(
    context: Context,
    summaryNotificationId: Int,
): PendingIntent = NotificationReceiver.downloadChildrenPendingBroadcast(
    context,
    originEntry,
    children.toTypedArray(),
    summaryNotificationId,
)

private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192
private const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
