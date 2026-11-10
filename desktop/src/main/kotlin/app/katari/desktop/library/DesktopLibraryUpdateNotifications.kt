package app.katari.desktop.library

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.link.LinkOpener
import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationAction
import app.katari.desktop.notification.NotificationProgress
import app.katari.desktop.notification.NotificationResponse
import app.katari.desktop.notification.NotificationUrgency
import app.katari.entry.interactions.library.EntryLibraryUpdateNotificationFeature
import app.katari.entry.interactions.library.EntryLibraryUpdateNotificationGroup
import app.katari.entry.interactions.library.EntryLibraryUpdateNotificationInput
import app.katari.entry.interactions.library.EntryLibraryUpdateQueueWarning
import app.katari.notification.AppNotificationKind
import app.katari.notification.LibraryUpdatesNotificationKind
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.library.label
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotificationProfile
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotificationProfiles
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import eu.kanade.tachiyomi.util.lang.chop
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.MetadataUpdateRunner
import tachiyomi.domain.library.update.model.LibraryUpdateProgress
import tachiyomi.i18n.MR
import tachiyomi.i18n.action_cancel
import tachiyomi.i18n.action_learn_more
import tachiyomi.i18n.action_show_errors
import tachiyomi.i18n.label_warning
import tachiyomi.i18n.localized.pluralStringResource
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.notification_large_update_warning
import tachiyomi.i18n.notification_library_update_next
import tachiyomi.i18n.notification_size_warning
import tachiyomi.i18n.notification_update_error
import tachiyomi.i18n.notification_updating_progress
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The notifications of one library update or metadata refresh, as Android posts them: the progress, replaced in place
 * and taken away when the update ends, the warning about checking much of one source, the failures, and what the
 * update found, which opens the profile's Updates. Under that summary Android adds a notification for each entry, in a
 * group; desktop notification services don't group, so the summary lists the entries, and the entries' own actions
 * stay on Android. A profile's name leads the text while there is more than one profile, as Android shows it in the
 * header, and titles stay out where the profile hides notification content.
 */
@Inject
class DesktopLibraryUpdateNotifications(
    private val context: PlatformContext,
    private val notifications: DesktopNotifications,
    private val notificationFeature: EntryLibraryUpdateNotificationFeature,
    private val notificationProfiles: LibraryUpdateNotificationProfiles,
    private val linkOpener: LinkOpener,
    // The queue and the runner build the updates this serves
    private val queue: Lazy<LibraryUpdateQueue>,
    private val metadataUpdateRunner: Lazy<MetadataUpdateRunner>,
) : LibraryUpdateNotifications {
    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    private var notificationProfile: LibraryUpdateNotificationProfile? = null
    private var lastProgressShown: TimeSource.Monotonic.ValueTimeMark? = null

    /** What the progress last showed, to show it again when the queue behind it changes. */
    private var lastProgress: Pair<List<Entry>, LibraryUpdateProgress>? = null

    /** Name of the profile whose update runs after this one. */
    private var nextInQueue: String? = null

    private suspend fun profile(profileId: Long): LibraryUpdateNotificationProfile {
        return notificationProfile?.takeIf { it.id == profileId }
            ?: notificationProfiles.resolve(profileId).also { notificationProfile = it }
    }

    override suspend fun showProgress(profileId: Long, entries: List<Entry>, progress: LibraryUpdateProgress) {
        showProgress(profile(profileId), entries, progress)
    }

    // Called from several update coroutines at once; updates closer together than the eye follows are skipped
    @Synchronized
    private fun showProgress(
        profile: LibraryUpdateNotificationProfile,
        entries: List<Entry>,
        progress: LibraryUpdateProgress,
    ) {
        lastProgress = entries.toList() to progress
        if (progress.completed < progress.total && !isProgressDue()) return
        val next = nextInQueue?.let { stringResource(MR.strings.notification_library_update_next, it) }
        notifications.show(
            PROGRESS,
            DesktopNotification(
                kind = AppNotificationKind.LIBRARY_PROGRESS,
                title = progress.label(context),
                body = lines(profile.label, entries.titles(profile), next),
                progress = NotificationProgress(progress.completed, progress.total),
                urgency = NotificationUrgency.Low,
                onClick = NotificationResponse.Open(HomeDestination.Library(profile.id)),
                actions = listOf(
                    NotificationAction(
                        stringResource(MR.strings.action_cancel),
                        NotificationResponse.Run { queue.value.stop(profile.id) },
                    ),
                ),
            ),
        )
    }

    override suspend fun showNextInQueue(profileId: Long, nextProfileId: Long?) {
        showNextInQueue(profile(profileId), nextProfileId?.let { notificationProfiles.name(it) })
    }

    @Synchronized
    private fun showNextInQueue(profile: LibraryUpdateNotificationProfile, profileName: String?) {
        if (nextInQueue == profileName) return
        nextInQueue = profileName
        val (entries, progress) = lastProgress ?: return
        lastProgressShown = null
        showProgress(profile, entries, progress)
    }

    override suspend fun showMetadataProgress(profileId: Long, entries: List<Entry>, completed: Int, total: Int) {
        showMetadataProgress(profile(profileId), entries, completed, total)
    }

    @Synchronized
    private fun showMetadataProgress(
        profile: LibraryUpdateNotificationProfile,
        entries: List<Entry>,
        completed: Int,
        total: Int,
    ) {
        if (completed < total && !isProgressDue()) return
        val percent = percentFormatter.format(completed.toFloat() / total)
        notifications.show(
            PROGRESS,
            DesktopNotification(
                kind = AppNotificationKind.LIBRARY_PROGRESS,
                title = stringResource(MR.strings.notification_updating_progress, percent),
                body = lines(profile.label, entries.titles(profile)),
                progress = NotificationProgress(completed, total),
                urgency = NotificationUrgency.Low,
                actions = listOf(
                    NotificationAction(
                        stringResource(MR.strings.action_cancel),
                        NotificationResponse.Run { metadataUpdateRunner.value.stop() },
                    ),
                ),
            ),
        )
    }

    /** Clicking the warning opens the rules of [rulesProfileId], or the explanation for an update they don't shape. */
    override suspend fun showQueueSizeWarningIfNeeded(
        profileId: Long,
        items: List<LibraryItem>,
        rulesProfileId: Long?,
    ) {
        val warning = notificationFeature.queueWarning(items.map(LibraryItem::entry))
        if (warning == EntryLibraryUpdateQueueWarning.NotRequired) return
        val profile = profile(profileId)
        val help = NotificationResponse.Run {
            linkOpener.open(context, LibraryUpdateNotifications.LARGE_UPDATE_HELP_URL)
        }
        val text = if (rulesProfileId != null) {
            MR.strings.notification_large_update_warning
        } else {
            MR.strings.notification_size_warning
        }
        notifications.show(
            WARNING,
            DesktopNotification(
                kind = AppNotificationKind.LIBRARY_PROGRESS,
                title = stringResource(MR.strings.label_warning),
                body = lines(profile.label, stringResource(text)),
                urgency = NotificationUrgency.Low,
                onClick = rulesProfileId?.let { NotificationResponse.Open(HomeDestination.LibraryUpdateSettings(it)) }
                    ?: help,
                actions = listOfNotNull(
                    NotificationAction(stringResource(MR.strings.action_learn_more), help)
                        .takeIf { rulesProfileId != null },
                ),
                timeout = WARNING_TIMEOUT,
            ),
        )
    }

    override suspend fun showUpdateErrors(profileId: Long, failed: Int) {
        if (failed == 0) return
        val profile = profile(profileId)
        notifications.show(
            errorsKey(profile.id),
            DesktopNotification(
                kind = AppNotificationKind.LIBRARY_ERRORS,
                title = pluralStringResource(MR.plurals.notification_update_error, failed, failed),
                body = lines(profile.label, stringResource(MR.strings.action_show_errors)),
                urgency = NotificationUrgency.Low,
                onClick = NotificationResponse.Open(HomeDestination.LibraryUpdateReport(profile.id)),
            ),
        )
    }

    override suspend fun showUpdates(profileId: Long, updates: List<Pair<Entry, List<EntryChapter>>>) {
        val profile = profile(profileId)
        val projection = notificationFeature.project(
            updates.map { (entry, children) -> EntryLibraryUpdateNotificationInput(entry, children) },
        )
        projection.omissions.forEach { omission ->
            logcat(LogPriority.DEBUG) {
                "Library-update notifications omitted ${omission.updateCount} ${omission.type} updates: " +
                    omission.reason
            }
        }
        projection.groups.forEach { showUpdates(profile, it) }
    }

    private fun showUpdates(profile: LibraryUpdateNotificationProfile, group: EntryLibraryUpdateNotificationGroup) {
        val titles = group.updates.map { it.originEntry.displayTitle.chop(UPDATE_TITLE_LENGTH) }
        val count = pluralStringResource(group.summary.text, titles.size, titles.size)
        val text = when {
            profile.hidesContent -> count
            titles.size == 1 -> titles.single()
            // Servers that show a long body don't scroll it
            else -> {
                val more = "…".takeIf { titles.size > LISTED_UPDATES }
                lines(count, *titles.take(LISTED_UPDATES).toTypedArray(), more)
            }
        }
        notifications.show(
            updatesKey(profile.id),
            DesktopNotification(
                kind = LibraryUpdatesNotificationKind(profile.id),
                title = stringResource(group.summary.title),
                body = lines(profile.label, text),
                onClick = NotificationResponse.Open(HomeDestination.Updates(profile.id)),
            ),
        )
    }

    override fun cancelProgress() {
        notifications.withdraw(PROGRESS)
    }

    private fun isProgressDue(): Boolean {
        val last = lastProgressShown
        if (last != null && last.elapsedNow() < PROGRESS_INTERVAL) return false
        lastProgressShown = TimeSource.Monotonic.markNow()
        return true
    }

    private fun List<Entry>.titles(profile: LibraryUpdateNotificationProfile): String? =
        takeUnless { profile.hidesContent }?.joinToString("\n") { it.title.chop(PROGRESS_TITLE_LENGTH) }

    private companion object {
        // One for every profile, as the queue runs one update at a time
        const val PROGRESS = "library-update-progress"
        const val WARNING = "library-update-warning"

        fun errorsKey(profileId: Long) = "library-update-errors-$profileId"

        fun updatesKey(profileId: Long) = "library-updates-$profileId"

        const val PROGRESS_TITLE_LENGTH = 40
        const val UPDATE_TITLE_LENGTH = 45
        const val LISTED_UPDATES = 5
        val PROGRESS_INTERVAL = 500.milliseconds
        val WARNING_TIMEOUT = 30.seconds
    }
}

private fun lines(vararg lines: String?): String? =
    lines.filterNot { it.isNullOrEmpty() }.joinToString("\n").ifEmpty { null }
