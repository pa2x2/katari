package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.data.BackupSystemWarning

/** Desktop systems have no setting known to break backups; they are read and written as plain files. */
@Inject
class NoBackupSystemWarning : BackupSystemWarning {
    override val applies: Boolean = false
}
