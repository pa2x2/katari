package eu.kanade.tachiyomi.data.backup.restore

import app.katari.core.common.platform.PlatformFacility

/** Where a restore reports how far it got and how it ended, such as Android's notifications. */
@PlatformFacility
interface BackupRestoreReporter {
    /** [content] names what was just restored; [sync] tells a library sync from a restore the user started. */
    fun onRestoreProgress(content: String, progress: Int, maxAmount: Int, sync: Boolean)

    /**
     * [time] is how long the restore took in milliseconds. The errors are listed in the file [file] in the directory
     * [path] when [errorCount] isn't 0.
     */
    fun onRestoreComplete(
        time: Long,
        errorCount: Int,
        path: String?,
        file: String?,
        sync: Boolean,
        restoredProfileNames: List<String>,
    )
}
