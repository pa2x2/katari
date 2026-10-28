package eu.kanade.presentation.more.settings

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.backup.create.BackupFileNames
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.media.EntryMediaCacheFeature
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StoragePreferences

/** Graph entry points of the settings screens. */
interface SettingsPresentationGraph {
    val backupCreateSchedule: BackupCreateSchedule
    val backupFileNames: BackupFileNames
    val backupPreferences: BackupPreferences
    val backupRestoreRunner: BackupRestoreRunner
    val basePreferences: BasePreferences
    val entryImmersiveFeature: EntryImmersiveFeature
    val entryMediaCacheFeature: EntryMediaCacheFeature
    val entryRepository: EntryRepository
    val sourceManager: SourceManager
    val storagePreferences: StoragePreferences
}
