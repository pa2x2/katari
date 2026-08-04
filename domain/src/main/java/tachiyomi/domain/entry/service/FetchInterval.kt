package tachiyomi.domain.entry.service

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import kotlin.math.absoluteValue
import kotlin.time.Instant

class FetchInterval(
    private val entryChapterRepository: EntryChapterRepository,
) {

    suspend fun update(
        entry: Entry,
        dateTime: LocalDateTime,
        timeZone: TimeZone,
        window: Pair<Long, Long>,
    ): Entry {
        val interval = entry.fetchInterval.takeIf { it < 0 } ?: calculateInterval(
            chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = true),
            zone = timeZone,
        )
        val currentWindow = if (window.first == 0L && window.second == 0L) {
            getWindow(dateTime.date, timeZone)
        } else {
            window
        }
        val nextUpdate = calculateNextUpdate(entry, interval, dateTime, timeZone, currentWindow)

        return entry.copy(nextUpdate = nextUpdate, fetchInterval = interval)
    }

    fun getWindow(localDate: LocalDate, timeZone: TimeZone): Pair<Long, Long> {
        val lowerBound = localDate.minus(GRACE_PERIOD, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        val upperBound = localDate.plus(GRACE_PERIOD, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        return Pair(lowerBound.toEpochMilliseconds(), upperBound.toEpochMilliseconds() - 1)
    }

    internal fun calculateInterval(chapters: List<EntryChapter>, zone: TimeZone): Int {
        val chapterWindow = if (chapters.size <= 8) 3 else 10

        val uploadDates = chapters.asSequence()
            .filter { it.dateUpload > 0L }
            .sortedByDescending { it.dateUpload }
            .map {
                Instant.fromEpochMilliseconds(it.dateUpload)
                    .toLocalDateTime(zone)
                    .date
            }
            .distinct()
            .take(chapterWindow)
            .toList()

        val fetchDates = chapters.asSequence()
            .sortedByDescending { it.dateFetch }
            .map {
                Instant.fromEpochMilliseconds(it.dateFetch)
                    .toLocalDateTime(zone)
                    .date
            }
            .distinct()
            .take(chapterWindow)
            .toList()

        val interval = when {
            // Enough upload date from source
            uploadDates.size >= 3 -> {
                val ranges = uploadDates.windowed(2).map { x -> x[1].daysUntil(x[0]) }.sorted()
                ranges[(ranges.size - 1) / 2]
            }
            // Enough fetch date from client
            fetchDates.size >= 3 -> {
                val ranges = fetchDates.windowed(2).map { x -> x[1].daysUntil(x[0]) }.sorted()
                ranges[(ranges.size - 1) / 2]
            }
            // Default to 7 days
            else -> 7
        }

        return interval.coerceIn(1, MAX_INTERVAL)
    }

    private fun calculateNextUpdate(
        entry: Entry,
        interval: Int,
        dateTime: LocalDateTime,
        timeZone: TimeZone,
        window: Pair<Long, Long>,
    ): Long {
        if (entry.nextUpdate in window.first.rangeTo(window.second + 1)) {
            return entry.nextUpdate
        }

        val instant = if (entry.lastUpdate > 0) {
            Instant.fromEpochMilliseconds(entry.lastUpdate)
        } else {
            dateTime.toInstant(timeZone)
        }
        val latestDate = instant.toLocalDateTime(timeZone).date

        val daysSinceLatest = latestDate.daysUntil(dateTime.date)
        val cycle = daysSinceLatest.floorDiv(
            interval.absoluteValue.takeIf { interval < 0 }
                ?: increaseInterval(interval, daysSinceLatest, increaseWhenOver = 10),
        )

        val offsetDays = (cycle + 1) * interval.absoluteValue
        return latestDate.plus(offsetDays, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
    }

    private fun increaseInterval(delta: Int, daysSinceLatest: Int, increaseWhenOver: Int): Int {
        if (delta >= MAX_INTERVAL) return MAX_INTERVAL

        // double delta again if missed more than 9 check in new delta
        val cycle = daysSinceLatest.floorDiv(delta) + 1
        return if (cycle > increaseWhenOver) {
            increaseInterval(delta * 2, daysSinceLatest, increaseWhenOver)
        } else {
            delta
        }
    }

    companion object {
        const val MAX_INTERVAL = 28

        private const val GRACE_PERIOD = 1
    }
}
