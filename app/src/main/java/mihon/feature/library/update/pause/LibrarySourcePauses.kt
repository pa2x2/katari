package mihon.feature.library.update.pause

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.SourceUpdatePause
import kotlin.time.Clock

/** Pauses and resumes library updates of whole sources. */
class LibrarySourcePauses(private val libraryPreferences: LibraryPreferences) {

    /** The pauses in effect at [now], by source id. */
    fun active(now: Long = currentTime()): Map<Long, SourceUpdatePause> {
        return libraryPreferences.updatePausedSources.get().activeAt(now)
    }

    /**
     * The pauses in effect whenever they change. A pause running out changes nothing stored, so it only shows once
     * something else changes them, such as the next update dropping it.
     */
    fun changes(): Flow<Map<Long, SourceUpdatePause>> {
        return libraryPreferences.updatePausedSources.changes().map { it.activeAt(currentTime()) }
    }

    /** @param until when the pause ends, in epoch milliseconds, or null to pause until the source is resumed. */
    fun pause(sourceId: Long, until: Long?) {
        update { pauses -> pauses.filterNot { it.sourceId == sourceId }.toSet() + SourceUpdatePause(sourceId, until) }
    }

    fun resume(sourceIds: Collection<Long>) {
        update { pauses -> pauses.filterNot { it.sourceId in sourceIds }.toSet() }
    }

    fun dropEnded(now: Long) {
        update { pauses -> pauses.filter { it.isActiveAt(now) }.toSet() }
    }

    private fun update(transform: (Set<SourceUpdatePause>) -> Set<SourceUpdatePause>) {
        val current = libraryPreferences.updatePausedSources.get()
        val updated = transform(current)
        if (updated != current) libraryPreferences.updatePausedSources.set(updated)
    }

    private fun Set<SourceUpdatePause>.activeAt(now: Long): Map<Long, SourceUpdatePause> {
        return filter { it.isActiveAt(now) }.associateBy(SourceUpdatePause::sourceId)
    }

    private fun currentTime() = Clock.System.now().toEpochMilliseconds()
}
