package tachiyomi.domain.entry.service.nextupdate

import dev.zacsweers.metro.Inject
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import kotlin.time.Instant

@Inject
class FetchInterval(
    private val entryChapterRepository: EntryChapterRepository,
) {

    /**
     * Works out the entry's fetch interval and the next day to check it for new chapters.
     *
     * @param window the current fetch window as epoch millis, `0L to 0L` for the one around [dateTime].
     * @param foundRelease whether the check that led here found new chapters.
     */
    suspend fun update(
        entry: Entry,
        dateTime: LocalDateTime,
        timeZone: TimeZone,
        window: Pair<Long, Long>,
        foundRelease: Boolean = false,
    ): Entry {
        val today = dateTime.date
        val currentWindow = if (window.first == 0L && window.second == 0L) getWindow(today, timeZone) else window
        val schedule = if (entry.fetchInterval < 0) {
            // The check that found a release is the entry's last update
            val lastUpdate = if (foundRelease) today else entry.lastUpdate.toLocalDateOrNull(timeZone)
            ReleaseSchedule.fixed(-entry.fetchInterval, lastUpdate ?: today)
        } else {
            val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = true)
            ReleaseSchedule.estimate(chapters, timeZone, today)
        }

        val expectedDay = entry.nextUpdate.toLocalDateOrNull(timeZone)
        // A release on or after the expected day means the next one is counted from it instead
        val isAwaitingRelease = expectedDay != null &&
            entry.nextUpdate in currentWindow.first..currentWindow.second + 1 &&
            (schedule.lastRelease == null || schedule.lastRelease < expectedDay)
        val nextUpdate = when {
            foundRelease -> schedule.nextCheckAfterRelease(today).atStartOfDayIn(timeZone).toEpochMilliseconds()
            isAwaitingRelease -> entry.nextUpdate
            else -> schedule.nextCheck(today).atStartOfDayIn(timeZone).toEpochMilliseconds()
        }

        return entry.copy(
            nextUpdate = nextUpdate,
            fetchInterval = if (entry.fetchInterval < 0) entry.fetchInterval else schedule.interval,
        )
    }

    fun getWindow(localDate: LocalDate, timeZone: TimeZone): Pair<Long, Long> {
        val lowerBound = localDate.minus(GRACE_PERIOD, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        val upperBound = localDate.plus(GRACE_PERIOD, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        return Pair(lowerBound.toEpochMilliseconds(), upperBound.toEpochMilliseconds() - 1)
    }

    // Entries store 0 for an unknown time
    private fun Long.toLocalDateOrNull(timeZone: TimeZone): LocalDate? {
        return takeIf { it > 0L }?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date }
    }

    companion object {
        const val MAX_INTERVAL = 28

        private const val GRACE_PERIOD = 1
    }
}
