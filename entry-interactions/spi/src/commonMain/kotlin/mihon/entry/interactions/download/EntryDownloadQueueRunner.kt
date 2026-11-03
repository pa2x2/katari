package mihon.entry.interactions.download

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps idle media downloaders available for new work until the whole queue has drained. */
internal suspend fun runEntryDownloadQueuesUntilIdle(processors: Collection<EntryDownloadProcessor>) = coroutineScope {
    val changed = Channel<Unit>(Channel.CONFLATED)
    fun run(processor: EntryDownloadProcessor): Job = launch {
        processor.runDownloadsUntilIdle()
    }.also { job -> job.invokeOnCompletion { changed.trySend(Unit) } }

    // Every provider must get its initial run, which also awaits its persisted queue restoration.
    val running = processors.associateWith(::run).toMutableMap()
    val observers = processors.map { processor ->
        launch {
            processor.queueState
                .map { queue -> queue.hasPendingDownloads() }
                .distinctUntilChanged()
                .collect { changed.trySend(Unit) }
        }
    }
    try {
        while (running.isNotEmpty()) {
            changed.receive()
            running.entries.removeAll { it.value.isCompleted }
            processors.forEach { processor ->
                if (processor !in running && processor.hasPendingDownloads()) {
                    running[processor] = run(processor)
                }
            }
        }
    } finally {
        observers.forEach { it.cancel() }
        changed.close()
    }
}

private fun List<EntryDownloadQueueGroup>.hasPendingDownloads(): Boolean =
    any { group -> group.items.any { it.state == EntryDownloadState.QUEUE } }
