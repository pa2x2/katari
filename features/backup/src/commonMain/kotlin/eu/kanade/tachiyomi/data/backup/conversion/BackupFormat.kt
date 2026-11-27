package eu.kanade.tachiyomi.data.backup.conversion

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import okio.BufferedSource

/**
 * The backups of one family of apps, which [BackupConverter] turns into Katari backups. Each format is its own class
 * contributed into the set the converter picks from, so supporting another app adds a class and changes nothing else.
 *
 * A format decides what of its apps' backups it carries and reports it all, including what it leaves behind.
 */
interface BackupFormat {
    /** The app the family is known by, as the tool names it. */
    val appName: String

    /** The other apps writing the same format, which the tool lists under [appName]. */
    val relatedAppNames: List<String>

    /**
     * Whether one of this format's apps could have written a file holding [contents]. A file two formats recognize is
     * left to the user to say which.
     */
    fun recognizes(contents: BackupFileContents): Boolean

    /**
     * Converts the backup, handing each Katari entry to [writeEntry] as soon as it is complete. [open] reads the backup
     * from its start, gunzipped where the file was gzipped, as often as the format needs to, and the format closes what
     * it opens. Throws when the content isn't a backup of this format after all.
     */
    suspend fun convert(open: () -> BufferedSource, writeEntry: (BackupEntry) -> Unit): ConvertedContent

    /** [metadata] is everything of the Katari backup but its entries. */
    class ConvertedContent(val metadata: Backup, val report: ConversionReport)
}
