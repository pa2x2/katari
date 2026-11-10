package app.katari.entry.interactions.runtime.usage

import app.katari.telemetry.Telemetry
import app.katari.telemetry.usage.ContentOrigin
import app.katari.telemetry.usage.ReadingSession
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * What one reader or player session reports when it closes, gathered as it runs: made when the reader starts opening,
 * told what it shows and completes, and ended once. The reader owns it for the life of the session, so it outlives
 * the reader's screen being rebuilt.
 */
class ReadingSessionReport internal constructor(
    private val entryType: EntryType,
    private val sourceKindOf: suspend (sourceId: Long) -> String?,
    private val scope: CoroutineScope,
) {
    private val openedAt = TimeSource.Monotonic.markNow()

    @Volatile
    private var sourceKind: String? = null
    private var openTime: Duration? = null
    private var shownDownloaded = false
    private var shownStreamed = false
    private val finished = mutableSetOf<Long>()
    private var ended = false

    /** Looks up the kind of extension [sourceId] comes from, which the report carries once the lookup is done. */
    fun sourceKnown(sourceId: Long) {
        scope.launch { sourceKind = sourceKindOf(sourceId) }
    }

    /** The reader showed a chapter, or a page, text or frame of one; the first call times the opening. */
    fun shown(downloaded: Boolean) = synchronized(this) {
        if (openTime == null) openTime = openedAt.elapsedNow()
        if (downloaded) shownDownloaded = true else shownStreamed = true
    }

    /** The reader completed [chapterId]; a chapter completed more than once in the session counts once. */
    fun finished(chapterId: Long) = synchronized(this) {
        finished += chapterId
    }

    /** Ends a reader's session, [readingMode] being the mode it showed at close. */
    fun endReading(readingMode: Enum<*>) {
        end(readingMode = readingMode.name.lowercase(), playerEngine = null, engineSwitched = null)
    }

    /** Ends the player's session, [engine] being the engine playing at close: null when no episode played. */
    fun endPlayback(engine: String?, engineSwitched: Boolean) {
        end(readingMode = null, playerEngine = engine, engineSwitched = engineSwitched.takeIf { engine != null })
    }

    private fun end(readingMode: String?, playerEngine: String?, engineSwitched: Boolean?) {
        val session = synchronized(this) {
            if (ended) return
            ended = true
            ReadingSession(
                entryType = entryType.name.lowercase(),
                readingMode = readingMode,
                playerEngine = playerEngine,
                engineSwitched = engineSwitched,
                sourceKind = sourceKind,
                contentOrigin = when {
                    shownDownloaded && shownStreamed -> ContentOrigin.Mixed
                    shownDownloaded -> ContentOrigin.Downloaded
                    shownStreamed -> ContentOrigin.Streamed
                    else -> null
                },
                duration = openedAt.elapsedNow(),
                finished = finished.size,
                openTime = openTime,
            )
        }
        Telemetry.reportReadingSession(session)
    }
}
