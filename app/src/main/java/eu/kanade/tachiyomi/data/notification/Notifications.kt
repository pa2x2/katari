package eu.kanade.tachiyomi.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import eu.kanade.tachiyomi.util.system.buildNotificationChannel
import eu.kanade.tachiyomi.util.system.buildNotificationChannelGroup
import mihon.entry.interactions.download.EntryDownloadNotifications
import mihon.entry.interactions.media.playback.VideoPlaybackNotifications
import mihon.entry.interactions.translate.EntryTranslateNotifications
import mihon.feature.profiles.core.Profile
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * Class to manage the basic information of all the notifications used in the app.
 */
object Notifications {

    /**
     * Common notification channel and ids used anywhere.
     */
    const val CHANNEL_COMMON = "common_channel"
    const val ID_DOWNLOAD_IMAGE = 2

    /**
     * Notification channel and ids used by the library updater.
     */
    private const val GROUP_LIBRARY = "group_library"
    const val CHANNEL_LIBRARY_PROGRESS = "library_progress_channel"
    const val ID_LIBRARY_PROGRESS = -101
    const val ID_LIBRARY_SIZE_WARNING = -103
    const val CHANNEL_LIBRARY_ERROR = "library_errors_channel"

    /**
     * New items get a channel, group and summary per profile, so one profile's alerts can be silenced alone and its
     * summary opens its own Updates. Failures get a notice per profile. Profile ids stay far below the gap between
     * the bases.
     */
    private const val CHANNEL_LIBRARY_UPDATES_PREFIX = "library_updates_channel_"
    private const val ID_LIBRARY_UPDATES_BASE = -10_000
    private const val ID_LIBRARY_ERROR_BASE = -20_000

    fun channelLibraryUpdates(profileId: Long) = "$CHANNEL_LIBRARY_UPDATES_PREFIX$profileId"

    fun groupLibraryUpdates(profileId: Long) = "mihon.entry.library_updates.$profileId"

    fun idLibraryUpdates(profileId: Long) = ID_LIBRARY_UPDATES_BASE - profileId.toInt()

    fun idLibraryError(profileId: Long) = ID_LIBRARY_ERROR_BASE - profileId.toInt()

    /** The channel all profiles shared, whose importance the per-profile channels start from. */
    private const val LEGACY_CHANNEL_LIBRARY_UPDATES = "library_updates_channel"

    /** Notification channels used by durable source migration work. */
    private const val GROUP_SOURCE_MIGRATION = "group_source_migration"
    const val CHANNEL_SOURCE_MIGRATION_PROGRESS = "source_migration_progress_channel"
    const val CHANNEL_SOURCE_MIGRATION_COMPLETE = "source_migration_complete_channel"

    /**
     * Notification channel and ids used by the downloader.
     */
    private const val GROUP_DOWNLOADER = "group_downloader"

    /**
     * Notification channel group of background chapter translation.
     */
    private const val GROUP_TRANSLATOR = "group_translator"

    /**
     * Notification channel and ids used by the backup/restore system.
     */
    private const val GROUP_BACKUP_RESTORE = "group_backup_restore"
    const val CHANNEL_BACKUP_RESTORE_PROGRESS = "backup_restore_progress_channel"
    const val ID_BACKUP_PROGRESS = -501
    const val ID_RESTORE_PROGRESS = -503
    const val CHANNEL_BACKUP_RESTORE_COMPLETE = "backup_restore_complete_channel_v2"
    const val ID_BACKUP_COMPLETE = -502
    const val ID_RESTORE_COMPLETE = -504

    /** Notification channels of the Statistics recaps, one per kind, so either can be silenced alone. */
    private const val GROUP_RECAPS = "group_recaps"
    const val CHANNEL_YEAR_RECAP = "year_recap_channel"
    const val CHANNEL_MONTH_RECAP = "month_recap_channel"
    const val ID_STATISTICS_RECAP = -801
    const val ID_STATISTICS_YEAR_RECAP = -802

    /**
     * Notification channel used for Incognito Mode
     */
    const val CHANNEL_INCOGNITO_MODE = "incognito_mode_channel"
    const val ID_INCOGNITO_MODE = -701

    /**
     * Notification channel and ids used for extension updates.
     */
    private const val GROUP_APK_UPDATES = "group_apk_updates"
    const val CHANNEL_EXTENSIONS_UPDATE = "ext_apk_update_channel"
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
     * Creates the notification channels introduced in Android Oreo.
     * This won't do anything on Android versions that don't support notification channels.
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
            listOf(
                buildNotificationChannelGroup(GROUP_BACKUP_RESTORE) {
                    setName(context.stringResource(MR.strings.label_backup))
                },
                buildNotificationChannelGroup(GROUP_DOWNLOADER) {
                    setName(context.stringResource(MR.strings.download_notifier_downloader_title))
                },
                buildNotificationChannelGroup(GROUP_TRANSLATOR) {
                    setName(context.stringResource(MR.strings.translation_title))
                },
                buildNotificationChannelGroup(GROUP_LIBRARY) {
                    setName(context.stringResource(MR.strings.label_library))
                },
                buildNotificationChannelGroup(GROUP_SOURCE_MIGRATION) {
                    setName(context.stringResource(MR.strings.label_migration))
                },
                buildNotificationChannelGroup(GROUP_APK_UPDATES) {
                    setName(context.stringResource(MR.strings.label_recent_updates))
                },
                buildNotificationChannelGroup(GROUP_RECAPS) {
                    setName(context.stringResource(MR.strings.statistics_recaps))
                },
            ),
        )

        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(CHANNEL_COMMON, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_common))
                },
                buildNotificationChannel(CHANNEL_LIBRARY_PROGRESS, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_progress))
                    setGroup(GROUP_LIBRARY)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_LIBRARY_ERROR, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_errors))
                    setGroup(GROUP_LIBRARY)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_SOURCE_MIGRATION_PROGRESS, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_progress))
                    setGroup(GROUP_SOURCE_MIGRATION)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_SOURCE_MIGRATION_COMPLETE, IMPORTANCE_DEFAULT) {
                    setName(context.stringResource(MR.strings.channel_complete))
                    setGroup(GROUP_SOURCE_MIGRATION)
                },
                buildNotificationChannel(EntryDownloadNotifications.CHANNEL_PROGRESS, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_progress))
                    setGroup(GROUP_DOWNLOADER)
                    setShowBadge(false)
                },
                buildNotificationChannel(EntryDownloadNotifications.CHANNEL_ERROR, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_errors))
                    setGroup(GROUP_DOWNLOADER)
                    setShowBadge(false)
                },
                buildNotificationChannel(EntryTranslateNotifications.CHANNEL_PROGRESS, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_progress))
                    setGroup(GROUP_TRANSLATOR)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_BACKUP_RESTORE_PROGRESS, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_progress))
                    setGroup(GROUP_BACKUP_RESTORE)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_BACKUP_RESTORE_COMPLETE, IMPORTANCE_HIGH) {
                    setName(context.stringResource(MR.strings.channel_complete))
                    setGroup(GROUP_BACKUP_RESTORE)
                    setShowBadge(false)
                    setSound(null, null)
                },
                buildNotificationChannel(CHANNEL_YEAR_RECAP, IMPORTANCE_DEFAULT) {
                    setName(context.stringResource(MR.strings.statistics_yearly_recap))
                    setGroup(GROUP_RECAPS)
                },
                buildNotificationChannel(CHANNEL_MONTH_RECAP, IMPORTANCE_DEFAULT) {
                    setName(context.stringResource(MR.strings.statistics_monthly_recap))
                    setGroup(GROUP_RECAPS)
                },
                buildNotificationChannel(VideoPlaybackNotifications.CHANNEL, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.channel_video_playback))
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_INCOGNITO_MODE, IMPORTANCE_LOW) {
                    setName(context.stringResource(MR.strings.pref_incognito_mode))
                },
                buildNotificationChannel(CHANNEL_EXTENSIONS_UPDATE, IMPORTANCE_DEFAULT) {
                    setGroup(GROUP_APK_UPDATES)
                    setName(context.stringResource(MR.strings.channel_ext_updates))
                },
            ),
        )
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
            buildNotificationChannel(channelLibraryUpdates(profile.id), legacy?.importance ?: IMPORTANCE_DEFAULT) {
                setName(
                    if (visibleProfiles.size > 1) {
                        context.stringResource(MR.strings.channel_library_updates_profile, profile.name)
                    } else {
                        context.stringResource(MR.strings.channel_library_updates)
                    },
                )
                setGroup(GROUP_LIBRARY)
            }
        }
        notificationManager.createNotificationChannelsCompat(channels)
        if (legacy != null) notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_LIBRARY_UPDATES)
        val kept = channels.mapTo(HashSet()) { it.id }
        notificationManager.notificationChannels
            .map { it.id }
            .filter { it.startsWith(CHANNEL_LIBRARY_UPDATES_PREFIX) && it !in kept }
            .forEach(notificationManager::deleteNotificationChannel)
    }
}
