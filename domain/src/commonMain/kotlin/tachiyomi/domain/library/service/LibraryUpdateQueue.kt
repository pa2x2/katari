package tachiyomi.domain.library.service

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState

/**
 * Every library update, of every profile, waits its turn in one queue, so profiles never update at the same time and
 * opening another profile leaves a started update in place. A profile has at most one update running or waiting.
 */
interface LibraryUpdateQueue {
    /** Checks what the update rules allow, limited to one page of the library when a scope is given. */
    suspend fun startNow(
        category: Category? = null,
        sourceId: Long? = null,
        entryType: EntryType? = null,
    ): LibraryUpdateStart

    /** Checks the picked entries whatever the rules say, leaving out only entries set to Never. */
    suspend fun startSelection(entryIds: Collection<Long>): LibraryUpdateStart

    suspend fun startSkippedOfLatest(): LibraryUpdateStart

    /** Queues the automatic update of [profileId], unless that profile already has an update running or waiting. */
    suspend fun startAutomatic(profileId: Long): LibraryUpdateStart

    /** Stops the update of the active profile, running or waiting; the next one in the queue goes ahead. */
    suspend fun stop()

    /** Stops the update of [profileId], running or waiting; the next one in the queue goes ahead. */
    suspend fun stop(profileId: Long)

    /**
     * The update of [profileId], or null when it has none. A running update shows up once it has decided to go ahead,
     * and a waiting one only while another profile's update runs.
     */
    fun stateFlow(profileId: Long): Flow<LibraryUpdateState?>
}
