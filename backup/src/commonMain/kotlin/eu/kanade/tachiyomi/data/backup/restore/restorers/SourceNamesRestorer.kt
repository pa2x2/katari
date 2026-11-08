package eu.kanade.tachiyomi.data.backup.restore.restorers

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager

/** Without a stub, a source that isn't installed has no name for the next backup to write. */
@Inject
class SourceNamesRestorer(
    private val sourceManager: SourceManager,
    private val stubSourceRepository: StubSourceRepository,
) {

    suspend operator fun invoke(backupSources: List<BackupSource>) {
        backupSources
            .filter { StubSource.isKnownName(it.name) }
            .filter {
                sourceManager.get(it.sourceId) == null && stubSourceRepository.getStubSource(it.sourceId) == null
            }
            .forEach { stubSourceRepository.upsertStubSource(it.sourceId, lang = "", name = it.name) }
    }
}
