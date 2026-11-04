package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.data.AndroidBackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.AndroidStorageVolumes
import eu.kanade.presentation.more.settings.screen.data.BackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.StorageVolumes

@BindingContainer
interface DataSettingsBindings {
    @Binds
    val AndroidBackupSystemWarning.backupSystemWarning: BackupSystemWarning

    @Binds
    val AndroidStorageVolumes.storageVolumes: StorageVolumes
}
