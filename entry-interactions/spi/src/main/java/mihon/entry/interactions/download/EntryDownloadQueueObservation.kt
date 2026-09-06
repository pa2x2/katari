package mihon.entry.interactions.download

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Projects a media downloader's mutable queue into current immutable snapshots.
 * Both status and progress must invalidate the projection, even when queue membership is unchanged.
 * Read the current queue on each invalidation so a delayed transfer event cannot resurrect removed work.
 * Every new collector gets the current queue, including progress acquired before it subscribed.
 */
fun <T> observeEntryDownloadQueue(
    queue: StateFlow<List<T>>,
    statusUpdates: Flow<T>,
    progressUpdates: Flow<T>,
    snapshot: (List<T>) -> List<EntryDownloadQueueGroup>,
): Flow<List<EntryDownloadQueueGroup>> = merge(
    queue.map { Unit },
    statusUpdates.map { Unit },
    progressUpdates.map { Unit },
).conflate()
    .map { snapshot(queue.value) }
    .distinctUntilChanged()
