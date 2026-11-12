package app.katari.notification

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/**
 * A kind of notification the app posts, which the person can turn off apart from the others. Android makes each a
 * notification channel and keeps the person's settings for it under [id]; the desktop has no such settings, so the
 * app keeps a switch for each in its own Notifications settings. Either way [id] never changes once released.
 */
sealed interface NotificationKind {
    val id: String
    val group: NotificationGroup?
}

/** Every kind but the new items of a profile's library update, which are one kind per profile. */
enum class AppNotificationKind(
    override val id: String,
    override val group: NotificationGroup?,
    val title: StringResource,
) : NotificationKind {
    COMMON("common_channel", null, MR.strings.channel_common),
    LIBRARY_PROGRESS("library_progress_channel", NotificationGroup.LIBRARY, MR.strings.channel_progress),
    LIBRARY_ERRORS("library_errors_channel", NotificationGroup.LIBRARY, MR.strings.channel_errors),
    DOWNLOAD_PROGRESS("downloader_progress_channel", NotificationGroup.DOWNLOADER, MR.strings.channel_progress),
    DOWNLOAD_ERRORS("downloader_error_channel", NotificationGroup.DOWNLOADER, MR.strings.channel_errors),
    TRANSLATION_PROGRESS("translation_progress_channel", NotificationGroup.TRANSLATOR, MR.strings.channel_progress),
    BACKUP_RESTORE_PROGRESS(
        "backup_restore_progress_channel",
        NotificationGroup.BACKUP_RESTORE,
        MR.strings.channel_progress,
    ),
    BACKUP_RESTORE_COMPLETE(
        "backup_restore_complete_channel_v2",
        NotificationGroup.BACKUP_RESTORE,
        MR.strings.channel_complete,
    ),
    SOURCE_MIGRATION_PROGRESS(
        "source_migration_progress_channel",
        NotificationGroup.SOURCE_MIGRATION,
        MR.strings.channel_progress,
    ),
    SOURCE_MIGRATION_COMPLETE(
        "source_migration_complete_channel",
        NotificationGroup.SOURCE_MIGRATION,
        MR.strings.channel_complete,
    ),
    MONTH_RECAP("month_recap_channel", NotificationGroup.RECAPS, MR.strings.statistics_monthly_recap),
    YEAR_RECAP("year_recap_channel", NotificationGroup.RECAPS, MR.strings.statistics_yearly_recap),
    EXTENSION_UPDATES("ext_apk_update_channel", NotificationGroup.EXTENSION_UPDATES, MR.strings.channel_ext_updates),

    /** Sync's questions waiting for an answer, and sync having stopped. */
    SYNC("sync_channel", null, MR.strings.sync_title),

    /** The progress of a long sync run, such as a first sync. */
    SYNC_PROGRESS("sync_progress_channel", null, MR.strings.sync_progress_channel),

    /** What keeps the app running, its notification only there because the platform needs one, which may be hidden. */
    SYNC_CONNECTION("sync_connection_channel", null, MR.strings.sync_connection_channel),
    VIDEO_PLAYBACK("video_playback_channel", null, MR.strings.channel_video_playback),
    INCOGNITO_MODE("incognito_mode_channel", null, MR.strings.pref_incognito_mode),
}

/** The new items one profile's library update found, kept apart per profile so one profile's can be silenced alone. */
data class LibraryUpdatesNotificationKind(val profileId: Long) : NotificationKind {
    override val id: String
        get() = "$ID_PREFIX$profileId"

    override val group: NotificationGroup
        get() = NotificationGroup.LIBRARY

    companion object {
        const val ID_PREFIX = "library_updates_channel_"
    }
}

/** Kinds shown together under a heading, in Android's notification settings as in the desktop's. */
enum class NotificationGroup(val id: String, val title: StringResource) {
    LIBRARY("group_library", MR.strings.label_library),
    DOWNLOADER("group_downloader", MR.strings.download_notifier_downloader_title),
    TRANSLATOR("group_translator", MR.strings.translation_title),
    BACKUP_RESTORE("group_backup_restore", MR.strings.label_backup),
    SOURCE_MIGRATION("group_source_migration", MR.strings.label_migration),
    RECAPS("group_recaps", MR.strings.statistics_recaps),
    EXTENSION_UPDATES("group_apk_updates", MR.strings.label_recent_updates),
}
