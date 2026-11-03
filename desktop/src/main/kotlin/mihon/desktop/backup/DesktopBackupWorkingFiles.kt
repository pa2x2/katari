package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import mihon.desktop.storage.DesktopDirectories
import java.io.File

@Inject
class DesktopBackupWorkingFiles(private val directories: DesktopDirectories) : BackupWorkingFiles {
    override val scratchDirectory: File
        get() = directories.cache.toFile()

    override fun newRestoreErrorLog(): File {
        val file = directories.cache.resolve("katari_restore_error.txt").toFile()
        file.delete()
        file.createNewFile()
        return file
    }
}
