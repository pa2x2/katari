package eu.kanade.tachiyomi.data.backup.conversion.newer

import java.io.IOException

/**
 * What converting found that a newer version of the app wrote than Katari knows (D123, D124): parts it doesn't know,
 * parts whose type changed, and the entries it couldn't read because of them. The report lists them (D127) and an issue
 * about them carries them (D131).
 */
data class NewerAppFindings(
    val unknownParts: List<UnknownPart> = emptyList(),
    val changedParts: List<ChangedPart> = emptyList(),
    val unreadableEntries: List<UnreadableEntry> = emptyList(),
    /** The format version the backup says it is in, for a format that writes one, as LNReader's does. */
    val formatVersion: Int? = null,
    /** The version of the app that wrote the backup, for a format that says, as LNReader's does. */
    val appVersion: String? = null,
) {
    val isEmpty: Boolean
        get() = unknownParts.isEmpty() && changedParts.isEmpty() && unreadableEntries.isEmpty()
}

/**
 * A field Katari doesn't know: [name] is its number, or its key in JSON; [where] is the message or file holding it,
 * from the backup or an entry inward; [kind] is how it is encoded; [times] is how many messages hold it.
 */
data class UnknownPart(val where: List<String>, val name: String, val kind: String, val times: Int)

/** A field Katari knows that holds another kind of value than Katari reads, [found] instead of [expected]. */
data class ChangedPart(
    val where: List<String>,
    val name: String,
    val expected: String,
    val found: String,
    val times: Int,
)

/**
 * An entry Katari couldn't read and left behind (D124): its [position] among the backup's entries counting from 1, its
 * [title] where that part could still be read, and why decoding it failed.
 */
data class UnreadableEntry(val position: Int, val title: String?, val reason: String)

/**
 * The backup, though whole, isn't laid out as Katari reads [app]'s backups: a newer version of the app wrote it (D129).
 * A file cut short or corrupt fails as damaged instead.
 */
class NewerAppBackupException(val app: String, cause: Throwable? = null) : IOException(cause)

/** Where a part is: the backup itself, or an entry. Nested messages add their field's name. */
const val BACKUP_PLACE = "backup"
const val ENTRY_PLACE = "entry"
internal val IN_BACKUP = listOf(BACKUP_PLACE)
internal val IN_ENTRY = listOf(ENTRY_PLACE)

/** The [UnknownPart.kind] of a file in a zip backup that Katari doesn't know. */
const val FILE_PART = "file"
