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
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import mihon.entry.interactions.EntryDownloadInteraction
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.math.RoundingMode
import java.text.NumberFormat

class LibraryUpdateNotifier(
    private val context: Context,
    private val securityPreferences: SecurityPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getMergedEntry: GetMergedEntry = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val entryDownloadInteraction: EntryDownloadInteraction = Injekt.get(),
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
        val maxUpdatesFromSource = entriesToUpdate
            .groupBy { it.entry.source }
            .filterKeys { sourceManager.get(it) !is UnmeteredSource }
            .maxOfOrNull { it.value.size } ?: 0

        if (maxUpdatesFromSource <= MANGA_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
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
        val childUpdates = runBlocking {
            updates.map { (entry, children) ->
                NotificationEntryUpdate(
                    originEntry = entry,
                    visibleEntry = getVisibleEntry(entry),
                    children = children,
                )
            }
        }

        childUpdates.groupBy { it.type }
            .forEach { (type, typeUpdates) ->
                showUpdateNotifications(type, typeUpdates)
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showUpdateNotifications(
        type: EntryUpdateNotificationType,
        childUpdates: List<NotificationEntryUpdate>,
    ) {
        // Parent group notification
        context.notify(
            type.summaryNotificationId,
            type.channel,
        ) {
            setContentTitle(type.summaryTitle(context))
            if (childUpdates.size == 1 && !securityPreferences.hideNotificationContent.get()) {
                setContentText(childUpdates.first().originEntry.displayTitle.chop(NOTIF_TITLE_MAX_LEN))
            } else {
                setContentText(
                    type.summaryText(context, childUpdates.size),
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

            setGroup(type.group)
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
                            ),
                        )
                    },
                )
            }
        }
    }

    private suspend fun createNewEntryNotification(
        update: NotificationEntryUpdate,
    ): Notification {
        val icon = getEntryIcon(update.originEntry)
        return context.notificationBuilder(update.type.channel) {
            setContentTitle(update.originEntry.displayTitle)

            val description = update.childDescription(context)
            setContentText(description)
            setStyle(NotificationCompat.BigTextStyle().bigText(description))

            setSmallIcon(R.drawable.ic_katari)

            if (icon != null) {
                setLargeIcon(icon)
            }

            setGroup(update.type.group)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            priority = NotificationCompat.PRIORITY_HIGH

            setContentIntent(update.openChildIntent(context))
            setAutoCancel(true)

            addAction(
                R.drawable.ic_done_24dp,
                update.type.markConsumedLabel(context),
                update.markConsumedIntent(context),
            )
            addAction(
                R.drawable.ic_book_24dp,
                update.type.viewEntryLabel(context),
                update.viewEntryIntent(context),
            )
            if (
                entryDownloadInteraction.supportsBulkDownload(update.originEntry) &&
                update.children.size <= CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD
            ) {
                addAction(
                    android.R.drawable.stat_sys_download_done,
                    context.stringResource(MR.strings.action_download),
                    update.downloadChildrenIntent(context),
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

    private suspend fun getVisibleEntry(entry: Entry): Entry {
        val visibleEntryId = getMergedEntry.awaitVisibleTargetId(entry.id)
        return getEntry.await(visibleEntryId) ?: entry
    }

    private data class NotificationEntryUpdate(
        val originEntry: Entry,
        val visibleEntry: Entry,
        val children: Array<EntryChapter>,
    ) {
        val type: EntryUpdateNotificationType = EntryUpdateNotificationType.from(originEntry)

        fun childDescription(context: Context): String {
            return type.childDescriptionProvider(context, children)
        }

        fun openChildIntent(context: Context): PendingIntent {
            return NotificationReceiver.openChildPendingActivity(context, visibleEntry, originEntry, children.first())
        }

        fun markConsumedIntent(context: Context): PendingIntent {
            return NotificationReceiver.markConsumedPendingBroadcast(
                context,
                originEntry,
                children,
                type.summaryNotificationId,
            )
        }

        fun viewEntryIntent(context: Context): PendingIntent {
            return NotificationReceiver.openEntryPendingActivity(
                context,
                visibleEntry.id,
                type.summaryNotificationId,
            )
        }

        fun downloadChildrenIntent(context: Context): PendingIntent {
            return NotificationReceiver.downloadChildrenPendingBroadcast(
                context,
                originEntry,
                children,
                type.summaryNotificationId,
            )
        }
    }

    private enum class EntryUpdateNotificationType(
        private val entryType: EntryType,
        val summaryNotificationId: Int,
        val channel: String,
        val group: String,
        private val summaryTitleProvider: (Context) -> String,
        private val summaryTextProvider: (Context, Int) -> String,
        val childDescriptionProvider: (Context, Array<EntryChapter>) -> String,
        private val markConsumedLabelProvider: (Context) -> String,
        private val viewEntryLabelProvider: (Context) -> String,
    ) {
        MANGA(
            entryType = EntryType.MANGA,
            summaryNotificationId = Notifications.ID_NEW_CHAPTERS,
            channel = Notifications.CHANNEL_NEW_CHAPTERS,
            group = Notifications.GROUP_NEW_CHAPTERS,
            summaryTitleProvider = { context ->
                context.stringResource(MR.strings.notification_new_chapters)
            },
            summaryTextProvider = { context, count ->
                context.pluralStringResource(MR.plurals.notification_new_chapters_summary, count, count)
            },
            childDescriptionProvider = { context, children ->
                context.getNewChaptersDescription(children)
            },
            markConsumedLabelProvider = { context ->
                context.stringResource(MR.strings.action_mark_as_read)
            },
            viewEntryLabelProvider = { context ->
                context.stringResource(MR.strings.action_view_chapters)
            },
        ),
        ANIME(
            entryType = EntryType.ANIME,
            summaryNotificationId = Notifications.ID_NEW_EPISODES,
            channel = Notifications.CHANNEL_NEW_EPISODES,
            group = Notifications.GROUP_NEW_EPISODES,
            summaryTitleProvider = { context ->
                context.stringResource(MR.strings.notification_new_episodes)
            },
            summaryTextProvider = { context, count ->
                context.pluralStringResource(MR.plurals.notification_new_episodes_summary, count, count)
            },
            childDescriptionProvider = { context, children ->
                context.getNewEpisodesDescription(children)
            },
            markConsumedLabelProvider = { context ->
                context.stringResource(MR.strings.action_mark_as_watched)
            },
            viewEntryLabelProvider = { context ->
                context.stringResource(MR.strings.action_view_episodes)
            },
        ),
        ;

        fun summaryTitle(context: Context): String {
            return summaryTitleProvider.invoke(context)
        }

        fun summaryText(context: Context, count: Int): String {
            return summaryTextProvider.invoke(context, count)
        }

        fun markConsumedLabel(context: Context): String {
            return markConsumedLabelProvider.invoke(context)
        }

        fun viewEntryLabel(context: Context): String {
            return viewEntryLabelProvider.invoke(context)
        }

        companion object {
            fun from(entry: Entry): EntryUpdateNotificationType {
                return entries.firstOrNull { it.entryType == entry.type } ?: MANGA
            }
        }
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

private fun Context.getNewChaptersDescription(chapters: Array<EntryChapter>): String {
    val displayableChapterNumbers = chapters
        .filter { it.isRecognizedNumber }
        .sortedBy { it.chapterNumber }
        .map { formatChapterNumber(it.chapterNumber) }
        .toSet()

    return when (displayableChapterNumbers.size) {
        0 -> {
            pluralStringResource(
                MR.plurals.notification_chapters_generic,
                chapters.size,
                chapters.size,
            )
        }
        1 -> {
            val remaining = chapters.size - displayableChapterNumbers.size
            if (remaining == 0) {
                stringResource(
                    MR.strings.notification_chapters_single,
                    displayableChapterNumbers.first(),
                )
            } else {
                stringResource(
                    MR.strings.notification_chapters_single_and_more,
                    displayableChapterNumbers.first(),
                    remaining,
                )
            }
        }
        else -> {
            val shouldTruncate = displayableChapterNumbers.size > NOTIF_MAX_CHAPTERS
            if (shouldTruncate) {
                val remaining = displayableChapterNumbers.size - NOTIF_MAX_CHAPTERS
                val joinedChapterNumbers = displayableChapterNumbers
                    .take(NOTIF_MAX_CHAPTERS)
                    .joinToString(", ")
                pluralStringResource(
                    MR.plurals.notification_chapters_multiple_and_more,
                    remaining,
                    joinedChapterNumbers,
                    remaining,
                )
            } else {
                stringResource(
                    MR.strings.notification_chapters_multiple,
                    displayableChapterNumbers.joinToString(", "),
                )
            }
        }
    }
}

private fun Context.getNewEpisodesDescription(chapters: Array<EntryChapter>): String {
    val displayableEpisodeNumbers = chapters
        .mapNotNull { chapter ->
            chapter.chapterNumber.takeIf { number -> number >= 0.0 }?.let(::formatChapterNumber)
        }
        .toSet()

    return when (displayableEpisodeNumbers.size) {
        0 -> {
            pluralStringResource(
                MR.plurals.notification_episodes_generic,
                chapters.size,
                chapters.size,
            )
        }
        1 -> {
            val remaining = chapters.size - displayableEpisodeNumbers.size
            if (remaining == 0) {
                stringResource(
                    MR.strings.notification_episodes_single,
                    displayableEpisodeNumbers.first(),
                )
            } else {
                stringResource(
                    MR.strings.notification_episodes_single_and_more,
                    displayableEpisodeNumbers.first(),
                    remaining,
                )
            }
        }
        else -> {
            val shouldTruncate = displayableEpisodeNumbers.size > NOTIF_MAX_EPISODES
            if (shouldTruncate) {
                val remaining = displayableEpisodeNumbers.size - NOTIF_MAX_EPISODES
                val joinedEpisodeNumbers = displayableEpisodeNumbers
                    .take(NOTIF_MAX_EPISODES)
                    .joinToString(", ")
                pluralStringResource(
                    MR.plurals.notification_episodes_multiple_and_more,
                    remaining,
                    joinedEpisodeNumbers,
                    remaining,
                )
            } else {
                stringResource(
                    MR.strings.notification_episodes_multiple,
                    displayableEpisodeNumbers.joinToString(", "),
                )
            }
        }
    }
}

private const val NOTIF_MAX_CHAPTERS = 5
private const val NOTIF_MAX_EPISODES = 5
private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192
private const val MANGA_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60
private const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
private const val CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 15
