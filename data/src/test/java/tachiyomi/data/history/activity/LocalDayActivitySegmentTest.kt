package tachiyomi.data.history.activity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class LocalDayActivitySegmentTest {

    @Test
    fun `split preserves duration at local midnight`() {
        val midnight = Instant.parse("2026-08-23T00:00:00Z").toEpochMilli()

        val result = splitActivityByLocalDay(
            startedAtEpochMillis = midnight - 45_000L,
            endedAtEpochMillis = midnight + 15_000L,
            durationMillis = 60_000L,
            timeZoneId = "UTC",
        )

        result.map { it.localDate to it.durationMillis } shouldBe listOf(
            "2026-08-22" to 45_000L,
            "2026-08-23" to 15_000L,
        )
        result.sumOf(LocalDayActivitySegment::durationMillis) shouldBe 60_000L
    }

    @Test
    fun `split follows experienced midnight across daylight saving transition`() {
        val start = Instant.parse("2026-03-28T22:30:00Z").toEpochMilli()
        val end = Instant.parse("2026-03-29T22:30:00Z").toEpochMilli()

        val result = splitActivityByLocalDay(
            startedAtEpochMillis = start,
            endedAtEpochMillis = end,
            durationMillis = end - start,
            timeZoneId = "Europe/Warsaw",
        )

        result.map(LocalDayActivitySegment::localDate) shouldBe listOf("2026-03-28", "2026-03-29", "2026-03-30")
        result.sumOf(LocalDayActivitySegment::durationMillis) shouldBe end - start
    }
}
