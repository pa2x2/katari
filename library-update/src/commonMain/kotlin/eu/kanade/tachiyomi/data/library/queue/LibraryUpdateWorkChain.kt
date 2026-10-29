package eu.kanade.tachiyomi.data.library.queue

import eu.kanade.tachiyomi.data.library.LibraryUpdateInput
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.update.model.LibraryUpdateProgress

/**
 * The platform's background work under the library update queue: one chain of updates that run one after another in
 * the order they were added, kept as the platform keeps its background work. [WorkChainLibraryUpdateQueue] holds the
 * queue's rules on top of it.
 */
interface LibraryUpdateWorkChain {
    /** The updates in the chain that haven't ended, running or waiting. */
    val updates: Flow<List<ChainedLibraryUpdate>>

    suspend fun currentUpdates(): List<ChainedLibraryUpdate>

    /** Adds an update of [input] behind every update in the chain. */
    suspend fun append(input: LibraryUpdateInput)

    /**
     * Takes [update] out of the chain without holding up or failing the updates behind it: a running one ends as
     * finished, a waiting one never runs.
     */
    suspend fun remove(update: ChainedLibraryUpdate)
}

/**
 * One update in a [LibraryUpdateWorkChain].
 *
 * @property profileId null for an update queued by a version that didn't record its profile.
 * @property queuedAt when it was added, in milliseconds since the epoch; [Long.MAX_VALUE] when unknown.
 * @property progress how far a running update got, once it has published any.
 */
data class ChainedLibraryUpdate(
    val id: String,
    val profileId: Long?,
    val queuedAt: Long,
    val isRunning: Boolean,
    val progress: LibraryUpdateProgress?,
)
