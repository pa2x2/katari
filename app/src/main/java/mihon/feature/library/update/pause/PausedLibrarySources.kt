package mihon.feature.library.update.pause

import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Names of the paused sources that still have entries in the library. A paused source without any has nothing to leave
 * out, and no screen lists it to resume, so it isn't worth pointing out.
 */
class PausedLibrarySources(
    private val sourcePauses: LibrarySourcePauses,
    private val sourceRepository: SourceRepository,
    private val sourceManager: SourceManager,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(sourcePauses.changes(), sourceRepository.getSourcesWithFavoriteCount()) { pauses, sources ->
            sources
                .filter { (source, count) -> count > 0 && source.id in pauses }
                .map { (source, _) -> sourceManager.getDisplayInfo(source.id).visualName() }
                .sortedBy { it.lowercase() }
        }
    }
}
