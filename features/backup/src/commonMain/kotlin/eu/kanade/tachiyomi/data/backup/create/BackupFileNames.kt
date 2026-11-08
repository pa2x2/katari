package eu.kanade.tachiyomi.data.backup.create

import app.katari.domain.app.AppIdentity
import dev.zacsweers.metro.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The names of the backup files this install creates, the ones it offers for a backup and its automatic ones. */
@Inject
class BackupFileNames(appIdentity: AppIdentity) {
    // Separate installs (release, foss, dev, ...) can share one storage folder, and pruning deletes every match of
    // the regex. Naming backups after the application ID keeps each install to its own files: "katari" for the
    // release build, "katari.foss" and so on for the others.
    private val prefix = appIdentity.applicationId.removePrefix("app.")
    private val ownBackupName = """${Regex.escape(prefix)}_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}\.tachibk""".toRegex()

    fun newFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH).format(Date())
        return "${prefix}_$date.tachibk"
    }

    /** Whether [fileName] is a name [newFileName] gives, so automatic backups may delete the file to make room. */
    fun isOwnBackup(fileName: String): Boolean = ownBackupName.matches(fileName)
}
