package eu.kanade.tachiyomi.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import app.katari.notification.AppNotificationKind
import app.katari.notification.LibraryUpdatesNotificationKind
import app.katari.notification.NotificationGroup
import app.katari.profiles.core.Profile
import eu.kanade.tachiyomi.util.system.buildNotificationChannel
import eu.kanade.tachiyomi.util.system.buildNotificationChannelGroup
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * The ids of the app's notifications, and the channels Android shows each [AppNotificationKind] in.
 */
object Notifications {

    const val ID_DOWNLOAD_IMAGE = 2

    const val ID_LIBRARY_PROGRESS = -101
    const val ID_LIBRARY_SIZE_WARNING = -103

    /**
     * New items get a channel, group and summary per profile, so one profile's alerts can be silenced alone and its
     * summary opens its own Updates. Failures get a notice per profile. Profile ids stay far below the gap between
     * the bases.
     */
    private const val ID_LIBRARY_UPDATES_BASE = -10_000
    private const val ID_LIBRARY_ERROR_BASE = -20_000

    fun groupLibraryUpdates(profileId: Long) = "app.katari.entry.library_updates.$profileId"

    fun idLibraryUpdates(profileId: Long) = ID_LIBRARY_UPDATES_BASE - profileId.toInt()

    fun idLibraryError(profileId: Long) = ID_LIBRARY_ERROR_BASE - profileId.toInt()

    /** The channel all profiles shared, whose importance the per-profile channels start from. */
    private const val LEGACY_CHANNEL_LIBRARY_UPDATES = "library_updates_channel"

    const val ID_BACKUP_PROGRESS = -501
    const val ID_RESTORE_PROGRESS = -503
    const val ID_BACKUP_COMPLETE = -502
    const val ID_RESTORE_COMPLETE = -504
    const val ID_RESTORE_EXTENSIONS = -505

    const val ID_STATISTICS_RECAP = -801
    const val ID_STATISTICS_YEAR_RECAP = -802

    /** Sync's notifications: questions waiting for an answer, and sync having stopped (D028). */
    const val ID_SYNC_QUESTIONS = -901
    const val ID_SYNC_STOPPED = -902

    /** The progress of a long run, such as a first sync, while it goes. */
    const val ID_SYNC_LONG_RUN = -903

    /** What keeps the app running for instant sync. */
    const val ID_SYNC_CONNECTION = -904

    /** A test message another device sent, which debug builds send. */
    const val ID_SYNC_TEST_MESSAGE = -905

    const val ID_INCOGNITO_MODE = -701

    const val ID_EXTENSION_UPDATES = -401
    const val ID_EXTENSION_INSTALLER = -402
    const val ID_EXTENSIONS_AUTO_UPDATED = -403

    private val deprecatedChannels = listOf(
        "downloader_channel",
        "downloader_complete_channel",
        "backup_restore_complete_channel",
        "library_channel",
        "library_progress_channel",
        "updates_ext_channel",
        "downloader_cache_renewal",
        "crash_logs_channel",
        "library_skipped_channel",
        // Per-type library-update channels consolidated into the shared library update route.
        "new_chapters_channel",
        "new_episodes_channel",
        // App updates download in the app since 1.13.0, without a notification.
        "app_apk_update_channel",
        // Replaced by a channel per recap kind.
        "statistics_channel",
    )

    /** Prefix of the derived per-type library-update channels superseded by the shared route. */
    private const val LEGACY_LIBRARY_UPDATE_CHANNEL_PREFIX = "entry_library_updates_"

    /** Legacy per-type library-update summary notifications replaced by the shared summary. */
    private val deprecatedSummaryIds = listOf(-301, -302)

    /**
     * Creates a notification channel for each [AppNotificationKind], in its [NotificationGroup], and deletes the
     * channels earlier versions created.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Delete old notification channels
        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)

        // Delete the derived per-type library-update channels superseded by the shared route
        notificationManager.notificationChannels
            .map { it.id }
            .filter { it.startsWith(LEGACY_LIBRARY_UPDATE_CHANNEL_PREFIX) }
            .forEach(notificationManager::deleteNotificationChannel)

        // Remove stale legacy per-type summary notifications replaced by the shared summary
        deprecatedSummaryIds.forEach(notificationManager::cancel)

        notificationManager.createNotificationChannelGroupsCompat(
            NotificationGroup.entries.map { group ->
                buildNotificationChannelGroup(group.id) { setName(context.stringResource(group.title)) }
            },
        )
        notificationManager.createNotificationChannelsCompat(
            AppNotificationKind.entries.map { kind -> channel(context, kind) },
        )
    }

    // Android applies a channel's importance, badge and sound only when it creates the channel; the user's choices win
    private fun channel(context: Context, kind: AppNotificationKind) =
        buildNotificationChannel(kind.id, kind.importance) {
            setName(context.stringResource(kind.title))
            kind.group?.let { setGroup(it.id) }
            if (!kind.showsBadge) setShowBadge(false)
            if (kind == AppNotificationKind.BACKUP_RESTORE_COMPLETE) setSound(null, null)
        }

    private val AppNotificationKind.importance: Int
        get() = when (this) {
            AppNotificationKind.COMMON,
            AppNotificationKind.LIBRARY_PROGRESS,
            AppNotificationKind.LIBRARY_ERRORS,
            AppNotificationKind.DOWNLOAD_PROGRESS,
            AppNotificationKind.DOWNLOAD_ERRORS,
            AppNotificationKind.TRANSLATION_PROGRESS,
            AppNotificationKind.BACKUP_RESTORE_PROGRESS,
            AppNotificationKind.SOURCE_MIGRATION_PROGRESS,
            AppNotificationKind.SYNC_PROGRESS,
            AppNotificationKind.SYNC_CONNECTION,
            AppNotificationKind.VIDEO_PLAYBACK,
            AppNotificationKind.INCOGNITO_MODE,
            -> IMPORTANCE_LOW
            AppNotificationKind.SOURCE_MIGRATION_COMPLETE,
            AppNotificationKind.MONTH_RECAP,
            AppNotificationKind.YEAR_RECAP,
            AppNotificationKind.EXTENSION_UPDATES,
            AppNotificationKind.SYNC,
            -> IMPORTANCE_DEFAULT
            AppNotificationKind.BACKUP_RESTORE_COMPLETE -> IMPORTANCE_HIGH
        }

    private val AppNotificationKind.showsBadge: Boolean
        get() = when (this) {
            AppNotificationKind.COMMON,
            AppNotificationKind.SOURCE_MIGRATION_COMPLETE,
            AppNotificationKind.MONTH_RECAP,
            AppNotificationKind.YEAR_RECAP,
            AppNotificationKind.EXTENSION_UPDATES,
            AppNotificationKind.SYNC,
            AppNotificationKind.INCOGNITO_MODE,
            -> true
            AppNotificationKind.LIBRARY_PROGRESS,
            AppNotificationKind.LIBRARY_ERRORS,
            AppNotificationKind.DOWNLOAD_PROGRESS,
            AppNotificationKind.DOWNLOAD_ERRORS,
            AppNotificationKind.TRANSLATION_PROGRESS,
            AppNotificationKind.BACKUP_RESTORE_PROGRESS,
            AppNotificationKind.BACKUP_RESTORE_COMPLETE,
            AppNotificationKind.SOURCE_MIGRATION_PROGRESS,
            AppNotificationKind.SYNC_PROGRESS,
            AppNotificationKind.SYNC_CONNECTION,
            AppNotificationKind.VIDEO_PLAYBACK,
            -> false
        }

    /**
     * Gives every visible profile its new-items channel, named after the profile once there is more than one, and
     * deletes the channels of profiles that were archived or deleted. Android restores a deleted channel's settings
     * when it is created again, so unarchiving brings back how the user had set it up.
     */
    fun syncLibraryUpdateChannels(context: Context, visibleProfiles: List<Profile>) {
        if (visibleProfiles.isEmpty()) return
        val notificationManager = NotificationManagerCompat.from(context)
        val legacy = notificationManager.getNotificationChannelCompat(LEGACY_CHANNEL_LIBRARY_UPDATES)
        val channels = visibleProfiles.map { profile ->
            // Only takes effect for channels that don't exist yet; Android keeps the user's choice otherwise.
            val kind = LibraryUpdatesNotificationKind(profile.id)
            buildNotificationChannel(kind.id, legacy?.importance ?: IMPORTANCE_DEFAULT) {
                setName(
                    if (visibleProfiles.size > 1) {
                        context.stringResource(MR.strings.channel_library_updates_profile, profile.name)
                    } else {
                        context.stringResource(MR.strings.channel_library_updates)
                    },
                )
                setGroup(kind.group.id)
            }
        }
        notificationManager.createNotificationChannelsCompat(channels)
        if (legacy != null) notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_LIBRARY_UPDATES)
        val kept = channels.mapTo(HashSet()) { it.id }
        notificationManager.notificationChannels
            .map { it.id }
            .filter { it.startsWith(LibraryUpdatesNotificationKind.ID_PREFIX) && it !in kept }
            .forEach(notificationManager::deleteNotificationChannel)
    }
}
