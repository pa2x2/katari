package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.data.BackupSystemWarning
import eu.kanade.presentation.more.settings.screen.data.StorageLocationPaths
import eu.kanade.presentation.more.settings.screen.data.StorageVolumes
import eu.kanade.tachiyomi.data.export.LibraryExportDocuments
import mihon.desktop.backup.NoBackupSystemWarning
import mihon.desktop.export.FileLibraryExportDocuments
import mihon.desktop.storage.DesktopStorageVolumes
import mihon.desktop.storage.FileStorageLocationPaths

@BindingContainer
interface DataSettingsBindings {
    @Binds
    val NoBackupSystemWarning.backupSystemWarning: BackupSystemWarning

    @Binds
    val FileStorageLocationPaths.storageLocationPaths: StorageLocationPaths

    @Binds
    val DesktopStorageVolumes.storageVolumes: StorageVolumes

    @Binds
    val FileLibraryExportDocuments.libraryExportDocuments: LibraryExportDocuments
}
