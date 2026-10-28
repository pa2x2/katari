package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.data.backup.extensions.BackedUpFileExtensions
import eu.kanade.tachiyomi.data.backup.preferences.PreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import mihon.desktop.backup.DesktopBackupCreateSchedule
import mihon.desktop.backup.DesktopBackupRestoreRunner
import mihon.desktop.backup.DesktopBackupWorkingFiles
import mihon.desktop.backup.DesktopPreferenceBackupStores
import mihon.desktop.backup.FileBackupDocuments
import mihon.desktop.backup.NoBackedUpFileExtensions
import mihon.desktop.backup.NoSourcePreferenceBackupStores
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupRestoreRunner

@BindingContainer
interface BackupBindings {
    @Binds
    val DesktopBackupCreateSchedule.backupCreateSchedule: BackupCreateSchedule

    @Binds
    val DesktopBackupRestoreRunner.backupRestoreRunner: BackupRestoreRunner

    @Binds
    val FileBackupDocuments.backupDocuments: BackupDocuments

    @Binds
    val DesktopBackupWorkingFiles.backupWorkingFiles: BackupWorkingFiles

    @Binds
    val DesktopPreferenceBackupStores.preferenceBackupStores: PreferenceBackupStores

    @Binds
    val NoSourcePreferenceBackupStores.sourcePreferenceBackupStores: SourcePreferenceBackupStores

    @Binds
    val NoBackedUpFileExtensions.backedUpFileExtensions: BackedUpFileExtensions
}
