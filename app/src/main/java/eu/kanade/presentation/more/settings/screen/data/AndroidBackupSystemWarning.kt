package eu.kanade.presentation.more.settings.screen.data

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.DeviceUtil

/** MIUI with its optimisation turned off, which breaks the document access backups go through. */
@Inject
class AndroidBackupSystemWarning : BackupSystemWarning {
    override val applies: Boolean
        get() = DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()
}
