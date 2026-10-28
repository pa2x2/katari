package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.extensions.BackedUpFileExtensions
import eu.kanade.tachiyomi.data.backup.models.BackupFileExtension
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Desktop cannot install extensions yet: a backup lists none, and a restore leaves out the ones a backup from Android
 * lists. Stands in until desktop hosts extensions.
 */
@Inject
class NoBackedUpFileExtensions : BackedUpFileExtensions {
    override fun installed(): List<BackupFileExtension> = emptyList()

    override fun reinstall(extensions: List<BackupFileExtension>) {
        if (extensions.isNotEmpty()) {
            logcat(LogPriority.INFO) { "Left out the backup's ${extensions.size} extensions, which desktop can't run" }
        }
    }
}
