package eu.kanade.tachiyomi.data.backup.conversion

import dev.icerock.moko.resources.StringResource

/**
 * What a conversion brought into the Katari backup and what it left behind, one row per kind of data. A kind the
 * backup holds none of has no row.
 */
data class ConversionReport(val rows: List<Row>) {
    data class Row(val kind: Kind, val carried: List<Item>, val leftBehind: List<Item>)

    /**
     * An entry, setting, store or other thing the backup held, by the name the other app gave it. [note] says what it
     * became in Katari when that isn't plain, or why it was left behind.
     */
    data class Item(val name: String, val note: StringResource? = null)

    enum class Kind {
        LIBRARY,
        CATEGORIES,
        PROGRESS_AND_HISTORY,
        MERGED_ENTRIES,
        CUSTOM_INFO,
        APP_SETTINGS,
        EXTENSION_SETTINGS,
        EXTENSION_STORES,
        INSTALLED_EXTENSIONS,
        TRACKERS,

        /** What only the other app has, such as saved searches or player buttons. */
        APP_SPECIFIC,
    }
}
