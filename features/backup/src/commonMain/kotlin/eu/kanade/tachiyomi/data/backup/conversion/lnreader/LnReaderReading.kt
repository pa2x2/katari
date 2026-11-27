package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import eu.kanade.tachiyomi.data.backup.models.BackupActivitySegment
import eu.kanade.tachiyomi.data.backup.models.BackupActivitySession
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import tachiyomi.data.history.activity.splitActivityByLocalDay
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * A novel's reading as Katari keeps it. Each chapter LNReader stamped with a read time is in history, and the time
 * spent on it becomes a reading session that ends then, so Statistics count it on that day (D032). LNReader keeps one
 * time per chapter, so a chapter read over several days counts on the last.
 *
 * LNReader writes read times in the device's local time without a zone, so they are read in [zone].
 */
internal class LnReaderReading(novel: LnReaderNovel, private val zone: ZoneId) {
    private val readAt: Map<String, Long> = novel.chapters
        .mapNotNull { chapter -> chapter.readTime?.let(::epochMillis)?.let { chapter.path to it } }
        .toMap()

    val history: List<BackupHistory> = novel.chapters.mapNotNull { chapter ->
        val at = readAt[chapter.path] ?: return@mapNotNull null
        BackupHistory(url = chapter.path, lastRead = at, readDuration = chapter.timeSpent?.coerceAtLeast(0) ?: 0)
    }

    val sessions: List<BackupActivitySession> = novel.chapters.mapNotNull { chapter ->
        val end = readAt[chapter.path] ?: return@mapNotNull null
        val duration = chapter.timeSpent?.takeIf { it > 0 } ?: return@mapNotNull null
        session(sessionId(novel, chapter), chapter.path, end - duration, end, duration)
    }

    /**
     * When Statistics started counting this reading, in epoch milliseconds: the start of its first session. Statistics
     * show a profile's days only from its epoch on, and restoring keeps the earliest epoch it's given, so without one
     * the sessions would sit before the profile's epoch and count as reading before Statistics were kept.
     */
    val statisticsEpoch: Long? = sessions.minOfOrNull(BackupActivitySession::startedAt)

    /** When the chapter was last read, in epoch milliseconds. */
    fun readAt(chapter: LnReaderChapter): Long? = readAt[chapter.path]

    private fun session(id: String, chapterUrl: String, start: Long, end: Long, duration: Long) = BackupActivitySession(
        sessionId = id,
        startedAt = start,
        endedAt = end,
        duration = duration,
        lastSequence = 1,
        segments = splitActivityByLocalDay(start, end, duration, zone.id).map { day ->
            BackupActivitySegment(
                chapterUrl = chapterUrl,
                localDate = day.localDate,
                timeZoneId = zone.id,
                startedAt = day.startedAtEpochMillis,
                endedAt = day.endedAtEpochMillis,
                duration = day.durationMillis,
            )
        },
    )

    // "2026-06-02 21:14:05": LNReader stamps it with SQLite's datetime('now', 'localtime') (HistoryQueries.ts)
    private fun epochMillis(text: String): Long? =
        runCatching { LocalDateTime.parse(text.trim().replaceFirst(' ', 'T')) }.getOrNull()
            ?.atZone(zone)?.toInstant()?.toEpochMilli()

    // The same chapter gets the same session each time the backup is converted, so restoring twice doesn't count twice
    private fun sessionId(novel: LnReaderNovel, chapter: LnReaderChapter): String =
        UUID.nameUUIDFromBytes("lnreader:${novel.pluginId}:${novel.path}:${chapter.path}".toByteArray()).toString()
}
