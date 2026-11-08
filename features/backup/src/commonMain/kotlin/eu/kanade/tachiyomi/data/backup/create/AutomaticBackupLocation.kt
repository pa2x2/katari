package eu.kanade.tachiyomi.data.backup.create

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/** The files automatic backups are written into, in the automatic backup folder, which keeps only the latest few. */
@Inject
class AutomaticBackupLocation(
    private val storageManager: StorageManager,
    private val fileNames: BackupFileNames,
    private val context: PlatformContext,
) {
    /** Null while there is no folder to back up into, when an automatic backup can't run. */
    fun folder(): StorageFile? = storageManager.getAutomaticBackupsDirectory()

    /** Deletes the oldest of this install's backups in [directory] to make room, and creates the next one there. */
    fun next(directory: StorageFile): DocumentUri {
        directory.listFiles()
            .orEmpty()
            .filter { file -> file.name?.let(fileNames::isOwnBackup) == true }
            .sortedByDescending { it.name }
            .drop(MAX_AUTO_BACKUPS - 1)
            .forEach { it.delete() }

        val file = directory.createFile(fileNames.newFileName())
            ?: throw IllegalStateException(context.stringResource(MR.strings.create_backup_file_error))
        return DocumentUri(file.uri)
    }

    private companion object {
        const val MAX_AUTO_BACKUPS: Int = 4
    }
}
