package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.data.AndroidBackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.AndroidStorageLocationPaths
import eu.kanade.presentation.more.settings.screen.data.AndroidStorageVolumes
import eu.kanade.presentation.more.settings.screen.data.BackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.StorageLocationPaths
import eu.kanade.presentation.more.settings.screen.data.StorageVolumes
import eu.kanade.tachiyomi.data.export.AndroidLibraryExportDocuments
import eu.kanade.tachiyomi.data.export.LibraryExportDocuments

@BindingContainer
interface DataSettingsBindings {
    @Binds
    val AndroidBackupSystemWarning.backupSystemWarning: BackupSystemWarning

    @Binds
    val AndroidStorageLocationPaths.storageLocationPaths: StorageLocationPaths

    @Binds
    val AndroidStorageVolumes.storageVolumes: StorageVolumes

    @Binds
    val AndroidLibraryExportDocuments.libraryExportDocuments: LibraryExportDocuments
}
