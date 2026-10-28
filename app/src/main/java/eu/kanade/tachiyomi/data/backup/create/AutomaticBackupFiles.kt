package eu.kanade.tachiyomi.data.backup.create

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.Inject
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** The files automatic backups are written into, in the backup folder, which keeps only the latest few. */
@Inject
class AutomaticBackupFiles(
    private val context: Context,
    private val fileNames: BackupFileNames,
) {
    /** Deletes the oldest of this install's backups in [directory] to make room, and creates the next one there. */
    fun create(directory: Uri): DocumentUri {
        val dir = UniFile.fromUri(context, directory)

        // Delete older backups
        dir?.listFiles { _, filename -> fileNames.isOwnBackup(filename) }
            .orEmpty()
            .sortedByDescending { it.name }
            .drop(MAX_AUTO_BACKUPS - 1)
            .forEach { it.delete() }

        // Create new file to place backup
        val file = dir?.createFile(fileNames.newFileName())
            ?: throw IllegalStateException(context.stringResource(MR.strings.create_backup_file_error))
        return DocumentUri(file.uri.toString())
    }

    private companion object {
        const val MAX_AUTO_BACKUPS: Int = 4
    }
}
