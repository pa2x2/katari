package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.data.backup.preferences.AndroidPreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.AndroidSourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.PreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.storage.AndroidBackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.AndroidBackupWorkingFiles
import eu.kanade.tachiyomi.data.backup.storage.AutomaticBackupFolder
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import tachiyomi.core.common.storage.UniFileStorageFile
import tachiyomi.domain.storage.service.StorageManager

/**
 * Where the shared backup code reads and writes on Android: documents, the app's files and preferences, and the
 * automatic backup folder.
 */
@BindingContainer
interface BackupBindings {
    @Binds
    val AndroidBackupDocuments.backupDocuments: BackupDocuments

    @Binds
    val AndroidBackupWorkingFiles.backupWorkingFiles: BackupWorkingFiles

    @Binds
    val AndroidPreferenceBackupStores.preferenceBackupStores: PreferenceBackupStores

    @Binds
    val AndroidSourcePreferenceBackupStores.sourcePreferenceBackupStores: SourcePreferenceBackupStores

    companion object {
        @Provides
        fun automaticBackupFolder(storageManager: StorageManager): AutomaticBackupFolder =
            AutomaticBackupFolder { storageManager.getAutomaticBackupsDirectory()?.let(::UniFileStorageFile) }
    }
}
