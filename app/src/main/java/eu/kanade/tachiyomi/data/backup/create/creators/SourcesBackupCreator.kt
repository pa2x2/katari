package eu.kanade.tachiyomi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.service.SourceManager

@Inject
class SourcesBackupCreator(
    private val sourceManager: SourceManager,
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
