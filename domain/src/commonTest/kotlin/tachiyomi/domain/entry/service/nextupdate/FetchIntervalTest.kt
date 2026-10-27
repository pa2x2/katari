package tachiyomi.domain.entry.service.nextupdate

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Execution(ExecutionMode.CONCURRENT)
class FetchIntervalTest {

    private val testTime = LocalDateTime.parse("2020-01-01T00:00:00")
    private val testTimeZone = TimeZone.UTC
    private val chapter = EntryChapter.create().copy(
        dateFetch = testTime.toInstant(testTimeZone).toEpochMilliseconds(),
        dateUpload = testTime.toInstant(testTimeZone).toEpochMilliseconds(),
    )

    @Test
    fun `interval falls back to 7 days without enough distinct release days`() {
        val sameDay = (1..50).map { chapterWithTime(1.days) }
        interval(sameDay) shouldBe 7
        interval(sameDay.map { it.copy(dateUpload = 0L) }) shouldBe 7
        interval((1..2).map { chapterWithTime(1.days) } + (1..5).map { chapterWithTime(2.days) }) shouldBe 7
    }

    @Test
    fun `interval follows the more recent chapters`() {
        val oldChapters = (1..5).map { chapterWithTime((it * 7).days) }
        val newChapters = (1..10).map { chapterWithTime(35.days + it.days) }
        interval(oldChapters + newChapters) shouldBe 1

        // With few chapters only the last few releases count, not the long gap before them
        val fewOld = (1..3).map { chapterWithTime((it * 7).days) }
        val fewNew = (1..3).map { chapterWithTime(21.days + 365.days + (it * 7).days) }
        interval(fewOld + fewNew) shouldBe 7
    }

    @Test
    fun `interval is floored to whole days and never below one day`() {
        interval((1..20).map { chapterWithTime((15 * it).hours) }) shouldBe 1

        val decimal = (1..5).map { chapterWithTime((25 * it).hours) }
        interval(decimal) shouldBe 1
        interval(decimal.map { it.copy(dateUpload = 0L) }) shouldBe 1

        interval((1..20).map { chapterWithTime((43 * it).hours) }) shouldBe 2
    }

    @Test
    fun `window includes the final millisecond before its upper local day`() {
        val timeZone = TimeZone.of("America/New_York")

        val window = FetchInterval(mockk()).getWindow(LocalDate(2020, 3, 8), timeZone)

        window.first shouldBe LocalDate(2020, 3, 7).atStartOfDayIn(timeZone).toEpochMilliseconds()
        window.second shouldBe LocalDate(2020, 3, 9).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1
    }

    @Test
    fun `next update remains at local start of day across daylight saving time`() = runTest {
        val timeZone = TimeZone.of("America/New_York")
        val entry = Entry.create().copy(fetchInterval = -1)

        val updated = FetchInterval(mockk()).update(
            entry = entry,
            dateTime = LocalDateTime(2020, 3, 8, 12, 0),
            timeZone = timeZone,
            window = 0L to 0L,
            foundRelease = true,
        )

        updated.nextUpdate shouldBe LocalDate(2020, 3, 9).atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    @Test
    fun `follows releases on a few fixed weekdays`() = runTest {
        val mondaysAndThursdays = generateSequence(LocalDate.parse("2019-12-02")) { it.plus(DatePeriod(days = 1)) }
            .takeWhile { it <= LocalDate.parse("2020-01-20") }
            .filter { it.dayOfWeek == DayOfWeek.MONDAY || it.dayOfWeek == DayOfWeek.THURSDAY }
            .toList()
        val chapters = chaptersOn(mondaysAndThursdays)

        ReleaseSchedule.estimate(chapters, testTimeZone, LocalDate.parse("2020-01-21")).pattern shouldBe
            ReleasePattern.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        nextUpdate(today = "2020-01-21", chapters) shouldBe LocalDate.parse("2020-01-23")
    }

    @Test
    fun `follows releases on days of the month`() = runTest {
        val dayOfMonth = listOf("2019-09-15", "2019-10-15", "2019-11-14", "2019-12-15", "2020-01-16")
        nextUpdate(today = "2020-01-20", chaptersOn(dayOfMonth, perDay = 2)) shouldBe LocalDate.parse("2020-02-15")

        // The last day of the month moves between the 28th and the 31st
        val lastDays = listOf("2019-09-30", "2019-10-31", "2019-11-30", "2019-12-31", "2020-01-31")
        nextUpdate(today = "2020-02-05", chaptersOn(lastDays, perDay = 2)) shouldBe LocalDate.parse("2020-02-29")

        val twiceMonthly = listOf(
            "2019-10-01",
            "2019-10-15",
            "2019-11-01",
            "2019-11-16",
            "2019-12-01",
            "2019-12-15",
            "2020-01-01",
            "2020-01-15",
        )
        val chapters = chaptersOn(twiceMonthly, perDay = 2)
        ReleaseSchedule.estimate(chapters, testTimeZone, LocalDate.parse("2020-01-20")).pattern shouldBe
            ReleasePattern.DaysOfMonth(setOf(1, 15))
        nextUpdate(today = "2020-01-20", chapters) shouldBe LocalDate.parse("2020-02-01")
    }

    @Test
    fun `follows releases on a weekday of the month`() = runTest {
        val firstSaturdays = listOf("2019-09-07", "2019-10-05", "2019-11-02", "2019-12-07", "2020-01-04")
        nextUpdate(today = "2020-01-10", chaptersOn(firstSaturdays, perDay = 2)) shouldBe
            LocalDate.parse("2020-02-01")

        val lastFridays = listOf("2019-09-27", "2019-10-25", "2019-11-29", "2019-12-27", "2020-01-31")
        nextUpdate(today = "2020-02-03", chaptersOn(lastFridays, perDay = 2)) shouldBe
            LocalDate.parse("2020-02-28")
    }

    @Test
    fun `follows releases in batches`() = runTest {
        // Three days of releases every three weeks
        val batches = every(days = 21, from = "2019-09-02", to = "2019-12-16")
            .flatMap { start -> (0..2).map { start.plus(DatePeriod(days = it)) } }
        val chapters = chaptersOn(batches.filter { it <= LocalDate.parse("2019-12-17") })

        nextUpdate(today = "2019-12-17", chapters, foundRelease = true) shouldBe LocalDate.parse("2019-12-18")
        nextUpdate(today = "2019-12-18", chaptersOn(batches), foundRelease = true) shouldBe
            LocalDate.parse("2020-01-06")
    }

    @Test
    fun `checks again around when an earlier break ended`() = runTest {
        // A season of weekly releases, a 98 day break, and another season
        val chapters = chaptersOn(weekly(from = "2019-01-02", to = "2019-03-27") + weekly("2019-07-03", "2019-09-25"))

        nextUpdate(today = "2019-12-20", chapters) shouldBe LocalDate.parse("2019-12-25")
        nextUpdate(today = "2019-12-26", chapters) shouldBe LocalDate.parse("2020-01-01")
    }

    @Test
    fun `never waits more than 28 days between checks`() = runTest {
        val lastDays = listOf("2019-09-30", "2019-10-31", "2019-11-30", "2019-12-31", "2020-01-31")

        nextUpdate(today = "2020-01-31", chaptersOn(lastDays, perDay = 2), foundRelease = true) shouldBe
            LocalDate.parse("2020-02-28")
    }

    @Test
    fun `keeps checking on the expected day until the release comes`() = runTest {
        val chapters = chaptersOn(weekly(from = "2019-12-04", to = "2019-12-25"))

        nextUpdate(today = "2020-01-01", chapters) shouldBe LocalDate.parse("2020-01-01")
        // Still in the window while nothing new came out
        nextUpdate(today = "2020-01-02", chapters, expecting("2020-01-01")) shouldBe LocalDate.parse("2020-01-01")
    }

    @Test
    fun `counts from a release on the expected day`() = runTest {
        val chapters = chaptersOn(weekly(from = "2019-12-04", to = "2020-01-01"))

        nextUpdate(today = "2020-01-01", chapters, expecting("2020-01-01")) shouldBe LocalDate.parse("2020-01-08")
    }

    @Test
    fun `early late and backdated releases leave the schedule in place`() = runTest {
        // Uploaded the day before it was found
        val backdated = chaptersOn(weekly(from = "2019-10-02", to = "2019-12-25") + LocalDate.parse("2019-12-31"))
        nextUpdate(today = "2020-01-01", backdated, expecting("2020-01-01"), foundRelease = true) shouldBe
            LocalDate.parse("2020-01-08")

        val early = chaptersOn(weekly(from = "2019-10-02", to = "2019-12-25") + LocalDate.parse("2019-12-30"))
        nextUpdate(today = "2019-12-30", early, expecting("2020-01-01"), foundRelease = true) shouldBe
            LocalDate.parse("2020-01-08")

        val late = chaptersOn(weekly(from = "2019-12-04", to = "2019-12-25") + LocalDate.parse("2020-01-03"))
        nextUpdate(today = "2020-01-03", late, expecting("2020-01-01"), foundRelease = true) shouldBe
            LocalDate.parse("2020-01-08")
    }

    @Test
    fun `keeps expecting the release after an extra one well before it`() = runTest {
        val chapters = chaptersOn(weekly(from = "2019-10-02", to = "2020-01-01") + LocalDate.parse("2020-01-02"))

        nextUpdate(today = "2020-01-02", chapters, expecting("2020-01-08"), foundRelease = true) shouldBe
            LocalDate.parse("2020-01-08")
    }

    @Test
    fun `follows the schedule once releases keep coming early`() = runTest {
        val history = every(days = 5, from = "2019-10-01", to = "2019-12-15")
        val chapters = chaptersOn(history + every(days = 5, from = "2019-12-19", to = "2019-12-29"))

        nextUpdate(today = "2019-12-29", chapters, expecting("2019-12-30"), foundRelease = true) shouldBe
            LocalDate.parse("2020-01-03")
    }

    @Test
    fun `keeps checking for an overdue release while recent ones came that late`() = runTest {
        val chapters = chaptersOn(afterGaps("2019-10-18", 6, 5, 8, 6, 5, 6, 8, 6, 5))

        // Due on the 18th from a median gap of 6, and gaps have run to 8
        nextUpdate(today = "2019-12-20", chapters, expecting("2019-12-18")) shouldBe LocalDate.parse("2019-12-20")
    }

    @Test
    fun `counts from an early release with a custom interval`() = runTest {
        val entry = expecting("2020-01-01").copy(fetchInterval = -7)

        nextUpdate(today = "2019-12-31", emptyList(), entry, foundRelease = true) shouldBe
            LocalDate.parse("2020-01-07")
    }

    @Test
    fun `spreads out checks during a hiatus and follows the schedule again once releases resume`() = runTest {
        val weeks = weekly(from = "2019-09-11", to = "2019-10-02")

        nextUpdate(today = "2020-01-01", chaptersOn(weeks)) shouldBe LocalDate.parse("2020-01-22")
        nextUpdate(
            today = "2020-01-01",
            chaptersOn(weeks + LocalDate.parse("2020-01-01")),
            expecting("2020-01-22"),
            foundRelease = true,
        ) shouldBe LocalDate.parse("2020-01-08")
    }

    @Test
    fun `ignores chapters dated in the future`() = runTest {
        val chapters = chaptersOn(weekly(from = "2019-12-04", to = "2019-12-25") + LocalDate.parse("2020-06-01"))

        nextUpdate(today = "2020-01-01", chapters) shouldBe LocalDate.parse("2020-01-01")
    }

    private suspend fun nextUpdate(
        today: String,
        chapters: List<EntryChapter>,
        entry: Entry = Entry.create(),
        foundRelease: Boolean = false,
    ): LocalDate {
        val repository = mockk<EntryChapterRepository>()
        coEvery { repository.getChaptersByEntryIdAwait(any(), any()) } returns chapters
        val fetchInterval = FetchInterval(repository)
        val date = LocalDate.parse(today)
        val updated = fetchInterval.update(
            entry = entry,
            dateTime = date.atTime(10, 0),
            timeZone = testTimeZone,
            window = fetchInterval.getWindow(date, testTimeZone),
            foundRelease = foundRelease,
        )
        return Instant.fromEpochMilliseconds(updated.nextUpdate).toLocalDateTime(testTimeZone).date
    }

    private fun expecting(day: String): Entry {
        return Entry.create().copy(
            nextUpdate = LocalDate.parse(day).atStartOfDayIn(testTimeZone).toEpochMilliseconds(),
        )
    }

    private fun interval(chapters: List<EntryChapter>): Int {
        return ReleaseSchedule.estimate(chapters, testTimeZone, LocalDate.parse("2021-06-01")).interval
    }

    private fun weekly(from: String, to: String) = every(days = 7, from, to)

    private fun every(days: Int, from: String, to: String): List<LocalDate> {
        return generateSequence(LocalDate.parse(from)) { it.plus(DatePeriod(days = days)) }
            .takeWhile { it <= LocalDate.parse(to) }
            .toList()
    }

    private fun afterGaps(from: String, vararg gaps: Int): List<LocalDate> {
        return gaps.runningFold(LocalDate.parse(from)) { day, gap -> day.plus(DatePeriod(days = gap)) }
    }

    @JvmName("chaptersOnDays")
    private fun chaptersOn(days: List<String>, perDay: Int = 1) = chaptersOn(days.map(LocalDate::parse), perDay)

    private fun chaptersOn(days: List<LocalDate>, perDay: Int = 1): List<EntryChapter> {
        return days.flatMap { day ->
            val time = (day.atStartOfDayIn(testTimeZone) + 12.hours).toEpochMilliseconds()
            List(perDay) { chapter.copy(dateFetch = time, dateUpload = time) }
        }
    }

    private fun chapterWithTime(duration: Duration): EntryChapter {
        val newTime = (testTime.toInstant(testTimeZone) + duration).toEpochMilliseconds()
        return chapter.copy(dateFetch = newTime, dateUpload = newTime)
    }
}
