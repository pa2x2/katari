package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Without a stub, a source that isn't installed has no name for the next backup to write. */
class SourceNamesRestorer(
    private val sourceManager: SourceManager = Injekt.get(),
    private val stubSourceRepository: StubSourceRepository = Injekt.get(),
) {

    suspend operator fun invoke(backupSources: List<BackupSource>) {
        backupSources
            // Some backups store the id as the name, and sources without a stub were backed up with no name.
            .filter { it.name.isNotBlank() && it.name.toLongOrNull() == null }
            .filter {
                sourceManager.get(it.sourceId) == null && stubSourceRepository.getStubSource(it.sourceId) == null
            }
            .forEach { stubSourceRepository.upsertStubSource(it.sourceId, lang = "", name = it.name) }
    }
}
