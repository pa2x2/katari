package eu.kanade.tachiyomi.data.backup.conversion

import dev.icerock.moko.resources.StringResource

/**
 * What a conversion brought into the Katari backup and what it left behind, one row per kind of data. A kind the
 * backup holds none of has no row.
 */
data class ConversionReport(val rows: List<Row>) {
    data class Row(val kind: Kind, val carried: List<Item>, val leftBehind: List<Item>)

    /** An entry, setting, store or other thing the backup held. [note] says why it was left behind, or how it changed. */
    data class Item(val name: Name, val note: StringResource? = null) {
        constructor(text: String, note: StringResource? = null) : this(Name.Text(text), note)
    }

    sealed interface Name {
        /** As the backup has it: a title, a store's name, an address. */
        data class Text(val text: String) : Name

        /** A setting by the key the other app keeps it under, which the tool names from its own table. */
        data class Setting(val key: String) : Name
    }

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
