package mihon.desktop.library

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState

/**
 * Stands in for library updates, which desktop cannot run until it can load sources. An update starts as Android's
 * does and ends at once, as Android's ends for a library with nothing to check, so no update is ever running or
 * waiting and stopping has nothing to stop. A queue that updates on desktop replaces this once desktop hosts
 * extensions.
 */
@Inject
class NoLibraryUpdateQueue : LibraryUpdateQueue {
    override suspend fun startNow(category: Category?, sourceId: Long?, entryType: EntryType?): LibraryUpdateStart =
        LibraryUpdateStart.Started

    override suspend fun startSelection(entryIds: Collection<Long>): LibraryUpdateStart = LibraryUpdateStart.Started

    override suspend fun startSkippedOfLatest(): LibraryUpdateStart = LibraryUpdateStart.Started

    override suspend fun startAutomatic(profileId: Long): LibraryUpdateStart = LibraryUpdateStart.Started

    override suspend fun stop() = Unit

    override suspend fun stop(profileId: Long) = Unit

    override fun stateFlow(profileId: Long): Flow<LibraryUpdateState?> = flowOf(null)
}
