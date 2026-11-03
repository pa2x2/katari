package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.preferences.PreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.storage.AutomaticBackupFolder
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import mihon.desktop.backup.DesktopBackupCreateSchedule
import mihon.desktop.backup.DesktopBackupRestoreRunner
import mihon.desktop.backup.DesktopBackupWorkingFiles
import mihon.desktop.backup.DesktopPreferenceBackupStores
import mihon.desktop.backup.FileBackupDocuments
import mihon.desktop.storage.StorageLocationFolders
import mihon.desktop.work.ResumableWork
import mihon.extension.runtime.jvm.preferences.DesktopSourcePreferenceBackupStores
import mihon.extension.runtime.jvm.preferences.DesktopSourcePreferenceFileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.source.service.SourceManager

@BindingContainer
interface BackupBindings {
    @Binds
    val DesktopBackupCreateSchedule.backupCreateSchedule: BackupCreateSchedule

    @Binds
    @IntoSet
    val DesktopBackupCreateSchedule.resumableBackups: ResumableWork

    @Binds
    val DesktopBackupRestoreRunner.backupRestoreRunner: BackupRestoreRunner

    @Binds
    @IntoSet
    val DesktopBackupRestoreRunner.resumableRestores: ResumableWork

    @Binds
    val FileBackupDocuments.backupDocuments: BackupDocuments

    @Binds
    val DesktopBackupWorkingFiles.backupWorkingFiles: BackupWorkingFiles

    @Binds
    val DesktopPreferenceBackupStores.preferenceBackupStores: PreferenceBackupStores

    companion object {
        @Provides
        fun automaticBackupFolder(folders: StorageLocationFolders): AutomaticBackupFolder =
            AutomaticBackupFolder(folders::automaticBackups)

        @Provides
        @SingleIn(AppScope::class)
        fun sourcePreferenceBackupStores(
            sourceManager: SourceManager,
            sourcePreferenceFiles: SourcePreferenceFiles,
            fileStore: DesktopSourcePreferenceFileStore,
        ): SourcePreferenceBackupStores = DesktopSourcePreferenceBackupStores(
            sourceManager,
            sourcePreferenceFiles,
            fileStore,
        )
    }
}
