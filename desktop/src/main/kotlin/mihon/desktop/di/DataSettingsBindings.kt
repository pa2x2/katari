package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.data.BackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.StorageVolumes
import mihon.desktop.backup.NoBackupSystemWarning
import mihon.desktop.storage.DesktopStorageVolumes

@BindingContainer
interface DataSettingsBindings {
    @Binds
    val NoBackupSystemWarning.backupSystemWarning: BackupSystemWarning

    @Binds
    val DesktopStorageVolumes.storageVolumes: StorageVolumes
}
