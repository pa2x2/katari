package app.katari.sync.client.setting.value

import tachiyomi.domain.library.service.LibraryPreferences

/**
 * The person-level preferences that hold the local IDs of other synced records, each in the format its owner stores
 * it in. Like [tachiyomi.domain.source.identity.SourceKeyedPreferences] for source keys, but for sync alone: backups
 * match categories by name when restoring instead.
 */
internal object LocalIdPreferences {
    enum class Target { CATEGORY, ENTRY }

    /** [value] of the preference [key], with every local ID in it replaced by what [transform] makes of it. */
    fun map(key: String, value: Any?, transform: (Target, Long) -> Long): Any? = when (key) {
        // -1 for none; 0 is the Default category
        LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY ->
            (value as? Int)?.takeIf { it >= 0 }?.let { transform(Target.CATEGORY, it.toLong()).toInt() } ?: value
        in CATEGORY_LISTS -> (value as? String)?.split(",")?.joinToString(",") { id ->
            id.toLongOrNull()?.let { transform(Target.CATEGORY, it).toString() } ?: id
        } ?: value
        // "<year>:<entry ID>"
        RECAP_HIDDEN_ENTRIES -> (value as? Set<*>)?.mapTo(LinkedHashSet()) { item ->
            val yearAndEntry = item as String
            val entryId = yearAndEntry.substringAfter(':', "").toLongOrNull() ?: return@mapTo yearAndEntry
            "${yearAndEntry.substringBefore(':')}:${transform(Target.ENTRY, entryId)}"
        } ?: value
        else -> value
    }

    // UpdatesPreferences and UpcomingPreferences, as comma-separated IDs
    private val CATEGORY_LISTS = setOf(
        "pref_filter_updates_included_categories",
        "pref_filter_updates_excluded_categories",
        "pref_filter_upcoming_included_categories",
        "pref_filter_upcoming_excluded_categories",
    )

    // StatisticsPreferences
    private const val RECAP_HIDDEN_ENTRIES = "statistics_recap_hidden_entries"
}
