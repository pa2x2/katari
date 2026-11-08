package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.data.backup.preferences.AndroidPreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.AndroidSourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.PreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.storage.AndroidBackupWorkingFiles
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles

/**
 * Where the shared backup code reads and writes on Android: documents, and the app's files and preferences.
 */
@BindingContainer
interface BackupBindings {
    @Binds
    val AndroidBackupWorkingFiles.backupWorkingFiles: BackupWorkingFiles

    @Binds
    val AndroidPreferenceBackupStores.preferenceBackupStores: PreferenceBackupStores

    @Binds
    val AndroidSourcePreferenceBackupStores.sourcePreferenceBackupStores: SourcePreferenceBackupStores
}
