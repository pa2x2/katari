package eu.kanade.tachiyomi.data.backup.conversion

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.models.BackupSource

/**
 * What a conversion brought into the Katari backup and what it left behind, one row per kind of data. A kind the
 * backup holds none of has no row.
 */
data class ConversionReport(val rows: List<Row>) {
    data class Row(val kind: Kind, val carried: List<Item>, val leftBehind: List<Item>) {
        companion object {
            /** The row, or null when the backup holds none of [kind]. */
            fun of(kind: Kind, carried: List<Item> = emptyList(), leftBehind: List<Item> = emptyList()): Row? =
                Row(kind, carried, leftBehind).takeIf { carried.isNotEmpty() || leftBehind.isNotEmpty() }
        }
    }

    /**
     * An entry, setting, store or other thing the backup held. [note] says what of it came over or was left behind, and
     * why, or how it changed.
     */
    data class Item(val name: Name, val note: StringResource? = null) {
        constructor(text: String, note: StringResource? = null) : this(Name.Text(text), note)
    }

    sealed interface Name {
        /** As the backup has it: a title, a store's name, an address. */
        data class Text(val text: String) : Name

        /** A Katari setting by its key, which the report names with Katari's title and where it is (D043, D053). */
        data class Setting(val key: String) : Name

        /**
         * A setting of the [app] the backup came from that Katari has nothing for: its [label] in that app and the
         * [place] it is kept there, such as Mihon's Privacy settings (D053, D055).
         */
        data class OtherAppSetting(val app: String, val place: List<StringResource>, val label: StringResource) : Name

        /** A part of the backup that has no name of its own, such as a server's settings. */
        data class Part(val label: StringResource) : Name
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

/**
 * How the report names a source: by its name, or by its id where the backup has none, as TachiyomiSY and Aniyomi write a
 * source whose extension isn't installed. Katari's restore names such a source by its id too.
 */
internal val BackupSource.reportName: String
    get() = name.ifEmpty { sourceId.toString() }
