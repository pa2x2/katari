package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * Passes state changes through at once and holds back the progress ticks of a chapter that is already downloading,
 * emitting only the latest tick of each chapter once per [period]. A chapter's held tick is dropped when its state
 * changes, so a late tick can't put a finished or cancelled download back to downloading.
 */
fun Flow<EntryDownloadStatus>.sampleProgress(period: Duration): Flow<EntryDownloadStatus> = channelFlow {
    val lock = Mutex()
    val downloading = HashSet<Pair<EntryType, Long>>()
    val heldTicks = LinkedHashMap<Pair<EntryType, Long>, EntryDownloadStatus>()
    var flush: Job? = null

    collect { status ->
        val key = status.entryType to status.chapterId
        lock.withLock {
            val isTick = status.state == EntryDownloadState.DOWNLOADING &&
                !status.persistedContentChanged &&
                !downloading.add(key)
            if (!isTick) {
                if (status.state != EntryDownloadState.DOWNLOADING) downloading.remove(key)
                heldTicks.remove(key)
                send(status)
                return@withLock
            }
            heldTicks[key] = status
            if (flush == null) {
                flush = launch {
                    delay(period)
                    lock.withLock {
                        heldTicks.values.forEach { send(it) }
                        heldTicks.clear()
                        flush = null
                    }
                }
            }
        }
    }
}
