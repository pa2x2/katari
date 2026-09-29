package tachiyomi.domain.entry.service

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class FetchIntervalTest {

    private val testTime = LocalDateTime.parse("2020-01-01T00:00:00")
    private val testTimeZone = TimeZone.UTC
    private var chapter = EntryChapter.create().copy(
        dateFetch = testTime.toInstant(testTimeZone).toEpochMilliseconds(),
        dateUpload = testTime.toInstant(testTimeZone).toEpochMilliseconds(),
    )

    private val fetchInterval = FetchInterval(mockk())

    @Test
    fun `returns default interval of 7 days when not enough distinct days`() {
        val chaptersWithUploadDate = (1..50).map {
            chapterWithTime(chapter, 1.days)
        }
        fetchInterval.calculateInterval(chaptersWithUploadDate, testTimeZone) shouldBe 7

        val chaptersWithoutUploadDate = chaptersWithUploadDate.map {
            it.copy(dateUpload = 0L)
        }
        fetchInterval.calculateInterval(chaptersWithoutUploadDate, testTimeZone) shouldBe 7
    }

    @Test
    fun `returns interval based on more recent chapters`() {
        val oldChapters = (1..5).map {
            chapterWithTime(chapter, (it * 7).days) // Would have interval of 7 days
        }
        val newChapters = (1..10).map {
            chapterWithTime(chapter, oldChapters.lastUploadDate() + it.days)
        }

        val chapters = oldChapters + newChapters

        fetchInterval.calculateInterval(chapters, testTimeZone) shouldBe 1
    }

    @Test
    fun `returns interval based on smaller subset of recent chapters if very few chapters`() {
        val oldChapters = (1..3).map {
            chapterWithTime(chapter, (it * 7).days)
        }
        // Significant gap between chapters
        val newChapters = (1..3).map {
            chapterWithTime(chapter, oldChapters.lastUploadDate() + 365.days + (it * 7).days)
        }

        val chapters = oldChapters + newChapters

        fetchInterval.calculateInterval(chapters, testTimeZone) shouldBe 7
    }

    @Test
    fun `interval is floored to whole days and never below one day`() {
        val subDay = (1..20).map { chapterWithTime(chapter, (15 * it).hours) }
        fetchInterval.calculateInterval(subDay, testTimeZone) shouldBe 1

        val decimal = (1..5).map { chapterWithTime(chapter, (25 * it).hours) }
        fetchInterval.calculateInterval(decimal, testTimeZone) shouldBe 1
        fetchInterval.calculateInterval(decimal.map { it.copy(dateUpload = 0L) }, testTimeZone) shouldBe 1

        val justBelowTwoDays = (1..20).map { chapterWithTime(chapter, (43 * it).hours) }
        fetchInterval.calculateInterval(justBelowTwoDays, testTimeZone) shouldBe 2
    }

    @Test
    fun `window includes the final millisecond before its upper local day`() {
        val timeZone = TimeZone.of("America/New_York")
        val localDate = LocalDate(2020, 3, 8)

        val window = fetchInterval.getWindow(localDate, timeZone)

        window.first shouldBe LocalDate(2020, 3, 7).atStartOfDayIn(timeZone).toEpochMilliseconds()
        window.second shouldBe LocalDate(2020, 3, 9).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1
    }

    @Test
    fun `next update remains at local start of day across daylight saving time`() = runTest {
        val timeZone = TimeZone.of("America/New_York")
        val latestDate = LocalDate(2020, 3, 7)
        val entry = Entry.create().copy(
            lastUpdate = latestDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            fetchInterval = -1,
        )

        val updated = fetchInterval.update(
            entry = entry,
            dateTime = LocalDateTime(2020, 3, 8, 12, 0),
            timeZone = timeZone,
            window = 0L to 0L,
        )

        updated.nextUpdate shouldBe LocalDate(2020, 3, 9).atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    private fun chapterWithTime(chapter: EntryChapter, duration: Duration): EntryChapter {
        val newTime = testTime.toInstant(testTimeZone).plus(duration).toEpochMilliseconds()
        return chapter.copy(dateFetch = newTime, dateUpload = newTime)
    }

    private fun List<EntryChapter>.lastUploadDate() =
        last().dateUpload.toDuration(DurationUnit.MILLISECONDS)
}
