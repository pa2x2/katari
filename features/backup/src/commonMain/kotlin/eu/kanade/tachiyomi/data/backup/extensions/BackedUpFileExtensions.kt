package eu.kanade.tachiyomi.data.backup.extensions

import eu.kanade.tachiyomi.data.backup.models.BackupFileExtension

/**
 * The extensions installed from files rather than as apps. A backup lists them by the store they came from, and
 * restoring installs them again from there.
 */
interface BackedUpFileExtensions {
    fun installed(): List<BackupFileExtension>

    /** Installs [extensions] from their stores in the background; the restore ends without waiting for the downloads. */
    fun reinstall(extensions: List<BackupFileExtension>)
}
