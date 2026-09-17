package mihon.entry.interactions.library

import tachiyomi.i18n.*

/**
 * Single shared Android identity for every library-update notification, regardless of content type.
 *
 * All participating content types post into this one channel, group, and summary so mixed-type
 * library updates surface as a single notification. The previous per-type identities
 * (`new_chapters_channel`, `new_episodes_channel`, and derived `entry_library_updates_*_channel`
 * channels with matching group keys and summary IDs) were revoked on consolidation and are deleted
 * by the app's channel setup on migration.
 */
internal val sharedLibraryUpdateNotificationRoute = EntryLibraryUpdateNotificationRoute(
    channelId = "library_updates_channel",
    channelLabel = MR.strings.channel_library_updates,
    groupKey = "mihon.entry.library_updates",
    summaryNotificationId = -303,
    summaryTitle = MR.strings.notification_new_items,
    summaryText = MR.plurals.notification_new_items_summary,
)
