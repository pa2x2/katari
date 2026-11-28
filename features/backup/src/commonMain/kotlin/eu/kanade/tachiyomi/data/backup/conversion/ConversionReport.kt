package eu.kanade.tachiyomi.data.backup.conversion

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.sourcekeys.backupSourceKey
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.i18n.*

/**
 * What a conversion brought into the Katari backup and what it left behind, one row per kind of data. A kind the
 * backup holds none of has no row.
 */
data class ConversionReport(val rows: List<Row>) {
    /** The keys of the app's settings no table knows, which a newer version of the app may have added (D129). */
    val unknownSettings: List<String>
        get() = rows.filter { it.kind == Kind.APP_SETTINGS }
            .flatMap { it.leftBehind }
            .filter { it.note?.text == MR.strings.backup_conversion_unknown_setting }
            .mapNotNull { (it.name as? Name.Text)?.text }

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
    data class Item(val name: Name, val note: Note? = null) {
        constructor(name: Name, note: StringResource?) : this(name, note?.let(::Note))

        constructor(text: String, note: StringResource? = null) : this(Name.Text(text), note)
    }

    /** A note's [text], filled in with [args], such as the category a category shares its place with (D116). */
    data class Note(val text: StringResource, val args: List<String> = emptyList())

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

        /**
         * A part of the backup that has no name of its own, such as a server's settings, or an entry that couldn't be
         * read by its place in the backup; [label] is filled in with [args].
         */
        data class Part(val label: StringResource, val args: List<String> = emptyList()) : Name

        /**
         * A part a newer version of the app wrote that Katari doesn't know (D127): [field], a field's number or key or a
         * file's name, as [kind] says, in the message or file at [where], from the backup or an entry inward.
         */
        data class NewerPart(val where: List<String>, val field: String, val kind: String) : Name

        /**
         * A source the backup's entries come from, by its name, null where the backup has none (D114), and the
         * [identity] a store lists its extension under, so the tool can look the extension up in the stores (D106).
         */
        data class Source(val text: String?, val identity: SourceIdentity) : Name
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

        /** What a newer version of the app wrote that Katari doesn't know (D127). */
        NEWER_PARTS,
    }
}

/**
 * How the report names a source's settings and the rules kept for it: by its name, or by its id where the backup has
 * none, as TachiyomiSY and Aniyomi write a source whose extension isn't installed.
 */
internal val BackupSource.reportName: String
    get() = name.ifEmpty { sourceId.toString() }

/** A source whose extension isn't carried: the person installs it from a store after restoring. */
internal val BackupSource.extensionItem: ConversionReport.Item
    get() = ConversionReport.Item(
        ConversionReport.Name.Source(name.ifEmpty { null }, identity),
        MR.strings.backup_conversion_install_from_store,
    )

/**
 * The extension settings row: one item per source with settings, named from [sourceNames] by the backup's source ID.
 * Yōkai writes a record for every source it has, settings or not.
 */
internal fun extensionSettingsRow(
    preferences: List<BackupSourcePreferences>,
    sourceNames: Map<Long, String>,
): ConversionReport.Row? = ConversionReport.Row.of(
    ConversionReport.Kind.EXTENSION_SETTINGS,
    carried = preferences.filter { it.prefs.isNotEmpty() }.map { source ->
        ConversionReport.Item(source.backupSourceKey?.let(sourceNames::get) ?: source.sourceKey)
    },
)
