package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * Frozen Android identity compatibility for routes that shipped before generic library-update notifications.
 *
 * This is not a support catalog. New content types always use derived routing and must never be added here.
 */
internal object LegacyLibraryUpdateNotificationRouteCompatibility {
    fun route(type: EntryType): LegacyRoute? = when (type) {
        EntryType.MANGA -> LegacyRoute(
            channelId = "new_chapters_channel",
            groupKey = "eu.kanade.tachiyomi.NEW_CHAPTERS",
            summaryNotificationId = -301,
        )
        EntryType.ANIME -> LegacyRoute(
            channelId = "new_episodes_channel",
            groupKey = "eu.kanade.tachiyomi.NEW_EPISODES",
            summaryNotificationId = -302,
        )
        else -> null
    }

    data class LegacyRoute(
        val channelId: String,
        val groupKey: String,
        val summaryNotificationId: Int,
    )
}

internal fun derivedLibraryUpdateSummaryNotificationId(typeKey: String): Int {
    val stableHash = typeKey.fold(17L) { hash, character -> (hash * 31L + character.code) and 0x3fff_ffffL }
    return -(10_000 + stableHash.toInt())
}

internal fun validateLibraryUpdateNotificationRoutes(
    routes: Map<EntryType, EntryLibraryUpdateNotificationRoute>,
) {
    fun <T> requireUnique(label: String, value: (EntryLibraryUpdateNotificationRoute) -> T) {
        val duplicates = routes.values.groupBy(value).filterValues { it.size > 1 }
        check(duplicates.isEmpty()) {
            "Library-update notification $label collision: " +
                duplicates.entries.joinToString { (key, values) -> "$key=${values.map { it.type }}" }
        }
    }
    requireUnique("channel", EntryLibraryUpdateNotificationRoute::channelId)
    requireUnique("group", EntryLibraryUpdateNotificationRoute::groupKey)
    requireUnique("summary ID", EntryLibraryUpdateNotificationRoute::summaryNotificationId)
}
