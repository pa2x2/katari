package eu.kanade.presentation.more.settings.screen.data

import app.katari.entry.interactions.media.EntryMediaCacheFeature
import eu.kanade.tachiyomi.data.backup.create.BackupFileNames
import eu.kanade.tachiyomi.data.export.LibraryExportDocuments
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.storage.service.StoragePreferences

/** Graph entry points of the data and storage settings and of creating and restoring backups. */
interface DataSettingsGraph {
    val backupCreateSchedule: BackupCreateSchedule
    val backupFileNames: BackupFileNames
    val backupPreferences: BackupPreferences
    val backupRestoreRunner: BackupRestoreRunner
    val backupSystemWarning: BackupSystemWarning
    val storagePreferences: StoragePreferences
    val storageLocationPaths: StorageLocationPaths
    val storageVolumes: StorageVolumes
    val entryMediaCacheFeature: EntryMediaCacheFeature

    /** Lists the library the CSV export writes. */
    val entryRepository: EntryRepository
    val libraryExportDocuments: LibraryExportDocuments
}
