package eu.kanade.presentation.more.settings

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.create.BackupFileNames
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.core.common.GlobalCustomPreferences
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.media.EntryMediaCacheFeature
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.MetadataUpdateRunner
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StoragePreferences

/** Graph entry points of the settings screens. */
interface SettingsPresentationGraph {
    val backupCreateSchedule: BackupCreateSchedule
    val backupFileNames: BackupFileNames
    val backupPreferences: BackupPreferences
    val backupRestoreRunner: BackupRestoreRunner
    val basePreferences: BasePreferences
    val bookDocumentReaderSettings: BookDocumentReaderSettings
    val entryDownloadMaintenanceFeature: EntryDownloadMaintenanceFeature
    val entryImmersiveFeature: EntryImmersiveFeature
    val entryMediaCacheFeature: EntryMediaCacheFeature
    val entryMergeConsequenceStatusFeature: EntryMergeConsequenceStatusFeature
    val entryRepository: EntryRepository
    val globalCustomPreferences: GlobalCustomPreferences
    val metadataUpdateRunner: MetadataUpdateRunner
    val networkHelper: NetworkHelper
    val networkPreferences: NetworkPreferences
    val privacyPreferences: PrivacyPreferences
    val progressiveImageEngine: ProgressiveImageEngine
    val progressiveImagePreferences: ProgressiveImagePreferences
    val securityPreferences: SecurityPreferences
    val sourceManager: SourceManager
    val sourceRepository: SourceRepository
    val storagePreferences: StoragePreferences
    val trustExtension: TrustExtension
}
