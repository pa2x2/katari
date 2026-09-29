package eu.kanade.tachiyomi.ui.stats

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsActivitySegment
import java.time.ZonedDateTime

class StatisticsActivityRhythmTest {

    @Test
    fun `active time spreads over local clock hours in the recorded zone`() {
        val start = ZonedDateTime.parse("2026-08-23T21:30:00+09:00[Asia/Tokyo]")
        val segment = StatisticsActivitySegment(
            type = EntryType.MANGA,
            localDate = "2026-08-23",
            startedAtEpochMillis = start.toInstant().toEpochMilli(),
            endedAtEpochMillis = start.plusHours(1L).toInstant().toEpochMilli(),
            // Paused for half of the wall-clock span; the active time is split in proportion.
            durationMillis = 1_800_000L,
            timeZoneId = "Asia/Tokyo",
        )

        val rhythm = buildActivityRhythm(listOf(segment), emptyList())

        rhythm.hourlyDurationMillis[21] shouldBe 900_000L
        rhythm.hourlyDurationMillis[22] shouldBe 900_000L
        rhythm.hourlyDurationMillis.sum() shouldBe 1_800_000L
    }
}
