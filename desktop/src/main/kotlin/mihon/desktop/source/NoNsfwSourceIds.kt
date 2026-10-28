package mihon.desktop.source

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.source.service.NsfwSourceIds

/** Desktop installs no extensions yet, so no source is known to have 18+ content. Stands in until it loads them. */
@Inject
class NoNsfwSourceIds : NsfwSourceIds {
    override fun subscribe(): Flow<Set<Long>> = flowOf(emptySet())
}
