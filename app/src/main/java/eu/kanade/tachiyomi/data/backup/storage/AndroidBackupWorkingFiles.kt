package eu.kanade.tachiyomi.data.backup.storage

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import java.io.File

@Inject
class AndroidBackupWorkingFiles(private val context: Context) : BackupWorkingFiles {
    override val scratchDirectory: File
        get() = context.cacheDir

    override fun newRestoreErrorLog(): File = context.createFileInCacheDir("katari_restore_error.txt")
}
