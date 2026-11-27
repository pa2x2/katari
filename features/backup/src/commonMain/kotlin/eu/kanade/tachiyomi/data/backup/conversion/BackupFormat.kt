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

    /** How surely one of this format's apps wrote a file holding [contents]; null when none did. */
    fun match(contents: BackupFileContents): Match?

    /**
     * Converts the backup [content], gunzipped where the file was gzipped, handing each Katari entry to [writeEntry] as
     * soon as it is complete. Throws when the content isn't a backup of this format after all.
     */
    suspend fun convert(content: BufferedSource, writeEntry: (BackupEntry) -> Unit): ConvertedContent

    enum class Match {
        /** The file has fields only this format's apps write. */
        CERTAIN,

        /** The file fits this format, but has nothing telling it from another format it fits as well. */
        POSSIBLE,
    }

    /** [metadata] is everything of the Katari backup but its entries. */
    class ConvertedContent(val metadata: Backup, val report: ConversionReport)
}
