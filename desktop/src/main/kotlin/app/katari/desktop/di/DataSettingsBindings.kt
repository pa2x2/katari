package app.katari.desktop.di

import app.katari.desktop.backup.NoBackupSystemWarning
import app.katari.desktop.storage.DesktopStorageVolumes
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.data.BackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.StorageVolumes

@BindingContainer
interface DataSettingsBindings {
    @Binds
    val NoBackupSystemWarning.backupSystemWarning: BackupSystemWarning

    @Binds
    val DesktopStorageVolumes.storageVolumes: StorageVolumes
}
