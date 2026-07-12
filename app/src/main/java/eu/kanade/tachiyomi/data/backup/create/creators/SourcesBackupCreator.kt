package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesBackupCreator(
    private val sourceManager: SourceManager = Injekt.get(),
) {

    operator fun invoke(entries: List<BackupEntry>): List<BackupSource> {
        return entries
            .asSequence()
            .map(BackupEntry::source)
            .distinct()
            .map(sourceManager::getOrStub)
            .map(UnifiedSource::toBackupSource)
            .toList()
    }
}

private fun UnifiedSource.toBackupSource() =
    BackupSource(
        name = name,
        sourceId = id,
    )
