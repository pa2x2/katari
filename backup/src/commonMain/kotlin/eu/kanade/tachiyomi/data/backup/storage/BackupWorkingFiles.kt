package eu.kanade.tachiyomi.data.backup.storage

import mihon.core.common.platform.PlatformFacility
import java.io.File

/** The app's own files that creating and restoring a backup need besides the backup itself. */
@PlatformFacility
interface BackupWorkingFiles {
    /** Where a backup being written keeps the parts it can only copy into the backup once they are complete. */
    val scratchDirectory: File

    /** A new, empty file to list what a restore could not restore in, replacing the previous restore's. */
    fun newRestoreErrorLog(): File
}
