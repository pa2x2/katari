package eu.kanade.tachiyomi.data.library.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateProgress
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.library.update.LibraryUpdateSettingsNavigation
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportNavigation
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationAction
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationDestination
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationFeature
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationGroup
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationInput
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationItem
import mihon.entry.interactions.library.EntryLibraryUpdateNotificationText
import mihon.entry.interactions.library.EntryLibraryUpdateQueueWarning
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource
import java.math.RoundingMode
import java.text.NumberFormat
import mihon.icons.materialsymbols.R as MaterialSymbolsR

@Inject
class LibraryUpdateNotifier(
    private val context: Context,
    private val notificationFeature: EntryLibraryUpdateNotificationFeature,
) {

    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /** One per notifier, which serves one job and so one profile. */
    private var progressNotificationBuilder: NotificationCompat.Builder? = null

    private var lastProgressNotificationTime = 0L

    /** What the library update progress last showed, to show it again when the queue behind it changes. */
    private var lastProgress: Pair<List<Entry>, LibraryUpdateProgress>? = null

    /** Name of the profile whose update runs after this one. */
    private var nextInQueue: String? = null

    private fun newProgressNotificationBuilder(
        profile: LibraryUpdateNotificationProfile,
        cancelIntent: PendingIntent,
        contentIntent: PendingIntent?,
    ): NotificationCompat.Builder {
        return context.notificationBuilder(Notifications.CHANNEL_LIBRARY_PROGRESS) {
            setContentTitle(context.stringResource(MR.strings.updating_library))
            setSubText(profile.label)
            setSmallIcon(MaterialSymbolsR.drawable.rounded_refresh)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            contentIntent?.let(::setContentIntent)
            addAction(
                MaterialSymbolsR.drawable.rounded_close,
                context.stringResource(MR.strings.action_cancel),
                cancelIntent,
            )
        }
            .also { progressNotificationBuilder = it }
    }

    /** The notification a library update starts in the foreground with. */
    fun libraryUpdateProgressNotification(profile: LibraryUpdateNotificationProfile): Notification {
        return libraryUpdateProgressBuilder(profile).build()
    }

    /** The notification a metadata refresh starts in the foreground with. */
    fun metadataProgressNotification(profile: LibraryUpdateNotificationProfile): Notification {
        return metadataProgressBuilder(profile).build()
    }

    private fun libraryUpdateProgressBuilder(profile: LibraryUpdateNotificationProfile) = progressNotificationBuilder
        ?: newProgressNotificationBuilder(
            profile,
            cancelIntent = NotificationReceiver.cancelLibraryUpdatePendingBroadcast(context, profile.id),
            contentIntent = openLibraryIntent(profile.id),
        )

    private fun metadataProgressBuilder(profile: LibraryUpdateNotificationProfile) = progressNotificationBuilder
        ?: newProgressNotificationBuilder(
            profile,
            cancelIntent = NotificationReceiver.cancelMetadataUpdatePendingBroadcast(context),
            contentIntent = null,
        )

    /**
     * Shows how far the library update got, the entries it is checking and whose update comes next.
     *
     * Called from several update coroutines at once, so it shares the builder under a lock and skips updates that
     * come faster than the system would show them anyway.
     */
    @Synchronized
    fun showProgressNotification(
        profile: LibraryUpdateNotificationProfile,
        entries: List<Entry>,
        progress: LibraryUpdateProgress,
    ) {
        lastProgress = entries.toList() to progress
        val now = SystemClock.elapsedRealtime()
        if (progress.completed < progress.total && now - lastProgressNotificationTime < NOTIF_PROGRESS_INTERVAL_MS) {
            return
        }
        lastProgressNotificationTime = now

        val next = nextInQueue?.let { context.stringResource(MR.strings.notification_library_update_next, it) }
        val updating = entries.takeUnless { profile.hidesContent }?.joinToString("\n") { it.title.chop(40) }
        val builder = libraryUpdateProgressBuilder(profile)
            .setContentTitle(progress.label(context))
            .setContentText(next)
            .setStyle(
                listOfNotNull(updating?.takeIf { it.isNotEmpty() }, next)
                    .takeIf { it.isNotEmpty() }
                    ?.let { NotificationCompat.BigTextStyle().bigText(it.joinToString("\n\n")) },
            )
        if (progress.total > 0) {
            builder.setProgress(progress.total, progress.completed, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        context.notify(Notifications.ID_LIBRARY_PROGRESS, builder.build())
    }

    /** Names the profile whose update starts after this one, or null when none waits. */
    @Synchronized
    fun showNextInQueue(profile: LibraryUpdateNotificationProfile, profileName: String?) {
        if (nextInQueue == profileName) return
        nextInQueue = profileName
        val (entries, progress) = lastProgress ?: return
        lastProgressNotificationTime = 0L
        showProgressNotification(profile, entries, progress)
    }

    /** Shows how far a metadata refresh got and the entries it is refreshing. */
    @Synchronized
    fun showMetadataProgressNotification(
        profile: LibraryUpdateNotificationProfile,
        entries: List<Entry>,
        current: Int,
        total: Int,
    ) {
        val now = SystemClock.elapsedRealtime()
        if (current < total && now - lastProgressNotificationTime < NOTIF_PROGRESS_INTERVAL_MS) return
        lastProgressNotificationTime = now

        val builder = metadataProgressBuilder(profile)
            .setContentTitle(
                context.stringResource(
                    MR.strings.notification_updating_progress,
                    percentFormatter.format(current.toFloat() / total),
                ),
            )
        if (!profile.hidesContent) {
            val updatingText = entries.joinToString("\n") { it.title.chop(40) }
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(updatingText))
        }
        context.notify(Notifications.ID_LIBRARY_PROGRESS, builder.setProgress(total, current, false).build())
    }

    /**
     * Warns when an update checks too much of one source.
     *
     * @param rulesProfileId the profile whose library update rules decided what is checked, so tapping the warning
     * opens them; null for updates the rules don't shape, whose warning links to the explanation instead.
     */
    suspend fun showQueueSizeWarningNotificationIfNeeded(
        profile: LibraryUpdateNotificationProfile,
        entriesToUpdate: List<LibraryItem>,
        rulesProfileId: Long?,
    ) {
        if (
            notificationFeature.queueWarning(entriesToUpdate.map(LibraryItem::entry)) ==
            EntryLibraryUpdateQueueWarning.NotRequired
        ) {
            return
        }

        val helpIntent = NotificationHandler.openUrl(context, HELP_WARNING_URL)
        context.notify(
            Notifications.ID_LIBRARY_SIZE_WARNING,
            Notifications.CHANNEL_LIBRARY_PROGRESS,
        ) {
            setContentTitle(context.stringResource(MR.strings.label_warning))
            setSubText(profile.label)
            setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_warning)
            setTimeoutAfter(WARNING_NOTIF_TIMEOUT_MS)
            if (rulesProfileId != null) {
                setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        context.stringResource(MR.strings.notification_large_update_warning),
                    ),
                )
                setContentIntent(
                    LibraryUpdateSettingsNavigation.pendingIntent(
                        context,
                        rulesProfileId,
                        Notifications.ID_LIBRARY_SIZE_WARNING,
                    ),
                )
                addAction(
                    MaterialSymbolsR.drawable.rounded_filled_warning,
                    context.stringResource(MR.strings.action_learn_more),
                    helpIntent,
                )
            } else {
                setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        context.stringResource(MR.strings.notification_size_warning),
                    ),
                )
                setContentIntent(helpIntent)
            }
        }
    }

    /**
     * Tells how many entries failed to update, opening the profile's update report.
     *
     * @param failed Number of entries that failed to update.
     */
    fun showUpdateErrorNotification(profile: LibraryUpdateNotificationProfile, failed: Int) {
        if (failed == 0) {
            return
        }

        context.notify(
            Notifications.idLibraryError(profile.id),
            Notifications.CHANNEL_LIBRARY_ERROR,
        ) {
            setContentTitle(context.pluralStringResource(MR.plurals.notification_update_error, failed, failed))
            setContentText(context.stringResource(MR.strings.action_show_errors))
            setSubText(profile.label)
            setSmallIcon(R.drawable.ic_katari)

            setContentIntent(LibraryUpdateReportNavigation.pendingIntent(context, profile.id))
            setAutoCancel(true)
        }
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of entries with new updates.
     */
    suspend fun showUpdateNotifications(
        profile: LibraryUpdateNotificationProfile,
        updates: List<Pair<Entry, Array<EntryChapter>>>,
    ) {
        val projection = notificationFeature.project(
            updates.map { (entry, children) ->
                EntryLibraryUpdateNotificationInput(
                    entry = entry,
                    children = children.toList(),
                )
            },
        )
        projection.omissions.forEach { omission ->
            logcat {
                "Library-update notifications omitted ${omission.updateCount} ${omission.type} updates: " +
                    omission.reason
            }
        }
        projection.groups.forEach { showUpdateNotifications(profile, it) }
    }

    private suspend fun showUpdateNotifications(
        profile: LibraryUpdateNotificationProfile,
        group: EntryLibraryUpdateNotificationGroup,
    ) {
        val channelId = Notifications.channelLibraryUpdates(profile.id)
        val groupKey = Notifications.groupLibraryUpdates(profile.id)
        val summaryId = Notifications.idLibraryUpdates(profile.id)
        val childUpdates = group.updates
        val title = context.stringResource(group.summary.title)
        val count = context.pluralStringResource(group.summary.text, childUpdates.size, childUpdates.size)
        // Parent group notification
        context.notify(summaryId, channelId) {
            setContentTitle(title)
            if (childUpdates.size == 1 && !profile.hidesContent) {
                setContentText(childUpdates.first().originEntry.displayTitle.chop(NOTIF_TITLE_MAX_LEN))
            } else {
                setContentText(count)

                if (!profile.hidesContent) {
                    setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            childUpdates.joinToString("\n") {
                                it.originEntry.displayTitle.chop(NOTIF_TITLE_MAX_LEN)
                            },
                        ),
                    )
                }
            }
            setSubText(profile.label)

            setSmallIcon(R.drawable.ic_katari)
            setLargeIcon(notificationBitmap)

            // What a lock screen that hides sensitive content shows instead of "Contents hidden".
            setPublicVersion(
                context.notificationBuilder(channelId) {
                    setContentTitle(title)
                    setContentText(count)
                    setSubText(profile.label)
                    setSmallIcon(R.drawable.ic_katari)
                }.build(),
            )

            setGroup(groupKey)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            setGroupSummary(true)
            priority = NotificationCompat.PRIORITY_HIGH

            setContentIntent(openUpdatesIntent(profile.id))
            setAutoCancel(true)
        }

        // Per-entry notification
        if (!profile.hidesContent) {
            context.notify(
                childUpdates.map { update ->
                    NotificationManagerCompat.NotificationWithIdAndTag(
                        update.originEntry.id.hashCode(),
                        createNewEntryNotification(profile, update, channelId, groupKey, summaryId),
                    )
                },
            )
        }
    }

    private suspend fun createNewEntryNotification(
        profile: LibraryUpdateNotificationProfile,
        update: EntryLibraryUpdateNotificationItem,
        channelId: String,
        groupKey: String,
        summaryId: Int,
    ): Notification {
        val icon = getEntryIcon(update.originEntry)
        return context.notificationBuilder(channelId) {
            setContentTitle(update.originEntry.displayTitle)

            val description = listOfNotNull(
                update.typeLabel?.let { context.stringResource(it) },
                context.render(update.description),
            )
                .joinToString(" · ")
            setContentText(description)
            setStyle(NotificationCompat.BigTextStyle().bigText(description))
            setSubText(profile.label)

            setSmallIcon(R.drawable.ic_katari)

            if (icon != null) {
                setLargeIcon(icon)
            }

            setGroup(groupKey)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            priority = NotificationCompat.PRIORITY_HIGH

            setContentIntent(update.destinationIntent(context, summaryId))
            setAutoCancel(true)

            if (EntryLibraryUpdateNotificationAction.MARK_CONSUMED in update.actions) {
                addAction(
                    MaterialSymbolsR.drawable.rounded_check,
                    context.stringResource(update.markConsumedLabel),
                    NotificationReceiver.markConsumedPendingIntent(
                        context,
                        update.originEntry,
                        update.children.toTypedArray(),
                        summaryId,
                        throughApp = profile.requiresUnlock,
                    ),
                )
            }
            if (EntryLibraryUpdateNotificationAction.VIEW_ENTRY in update.actions) {
                addAction(
                    MaterialSymbolsR.drawable.rounded_book,
                    context.stringResource(update.viewChildrenLabel),
                    update.viewEntryIntent(context, summaryId),
                )
            }
            if (EntryLibraryUpdateNotificationAction.DOWNLOAD in update.actions) {
                addAction(
                    android.R.drawable.stat_sys_download_done,
                    context.stringResource(MR.strings.action_download),
                    NotificationReceiver.downloadChildrenPendingIntent(
                        context,
                        update.originEntry,
                        update.children.toTypedArray(),
                        summaryId,
                        throughApp = profile.requiresUnlock,
                    ),
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

    /** Opens [action] in the profile, switching to it and asking for its unlock first when needed. */
    private fun openInProfileIntent(action: String, profileId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this.action = action
            putExtra(Constants.PROFILE_EXTRA, profileId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openUpdatesIntent(profileId: Long) =
        openInProfileIntent(Constants.SHORTCUT_UPDATES, profileId, Notifications.idLibraryUpdates(profileId))

    private fun openLibraryIntent(profileId: Long) =
        openInProfileIntent(Constants.SHORTCUT_LIBRARY, profileId, Notifications.ID_LIBRARY_PROGRESS)

    companion object {
        const val HELP_WARNING_URL = "${Constants.URL_DOCS}library-updates/#large-updates"
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

private fun EntryLibraryUpdateNotificationItem.viewEntryIntent(
    context: Context,
    summaryNotificationId: Int,
): PendingIntent = NotificationReceiver.openEntryPendingActivity(
    context,
    visibleEntry.profileId,
    visibleEntry.id,
    summaryNotificationId,
)

private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192
private const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
private const val NOTIF_PROGRESS_INTERVAL_MS = 500L
