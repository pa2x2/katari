package eu.kanade.tachiyomi.ui.stats.recap.story

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import mihon.entry.interactions.statistics.EntryStatisticsContribution
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity
import tachiyomi.domain.statistics.recap.StatisticsRecapEntry
import tachiyomi.domain.statistics.recap.StatisticsRecapPeriodTotals
import tachiyomi.domain.statistics.recap.StatisticsRecapSegment
import tachiyomi.i18n.*
import java.time.LocalDate
import java.time.ZoneOffset

class StatisticsRecapStoryTest {

    @Test
    fun `a hidden title is never named or shown but its time still counts`() {
        val activity = activity(
            firstActive = "2026-01-02",
            1L to listOf("2026-01-02" to HOUR * 5, "2026-03-04" to HOUR * 4),
            2L to listOf("2026-01-03" to HOUR * 2),
            3L to listOf("2026-02-01" to HOUR),
        )

        val story = buildStatisticsRecapStory(YEAR_2026, activity, null, hiddenEntryIds = setOf(1L), CONTRIBUTIONS)

        story.page<StatisticsRecapPage.TotalTime>().durationMillis shouldBe HOUR * 12
        story.page<StatisticsRecapPage.TopTitle>().title.entryId shouldBe 2L
        story.page<StatisticsRecapPage.Opening>().covers.map { it.entryId } shouldBe listOf(2L, 3L)
        story.pages.filterIsInstance<StatisticsRecapPage.Bookend>().map { it.title.entryId } shouldBe listOf(2L, 3L)
        story.page<StatisticsRecapPage.BiggestDay>().let { day ->
            day.date shouldBe LocalDate.parse("2026-01-02")
            day.title shouldBe null
        }
        story.summary.durationMillis shouldBe HOUR * 12
        story.summary.titles.map { it.entryId } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `a year is compared only with a previous year that was tracked throughout`() {
        val segments = 1L to listOf("2026-05-01" to HOUR * 3)
        val previous = StatisticsRecapPeriod.Year(2025) to StatisticsRecapPeriodTotals(HOUR * 2, topEntryId = 1L)

        val trackedSinceAugust = buildStatisticsRecapStory(
            YEAR_2026,
            activity(firstActive = "2025-08-23", segments),
            previous,
            emptySet(),
            CONTRIBUTIONS,
        )
        val trackedAllYear = buildStatisticsRecapStory(
            YEAR_2026,
            activity(firstActive = "2025-01-05", segments),
            previous,
            emptySet(),
            CONTRIBUTIONS,
        )

        trackedSinceAugust.pages.none { it is StatisticsRecapPage.Comparison } shouldBe true
        trackedAllYear.page<StatisticsRecapPage.Comparison>().let { comparison ->
            comparison.changePercent shouldBe 50
            comparison.sameTopTitle?.entryId shouldBe 1L
        }
    }

    private inline fun <reified T : StatisticsRecapPage> StatisticsRecapStory.page(): T =
        pages.filterIsInstance<T>().single()

    private fun activity(
        firstActive: String,
        vararg days: Pair<Long, List<Pair<String, Long>>>,
    ): StatisticsRecapActivity {
        val segments = days.flatMap { (entryId, entryDays) ->
            entryDays.map { (date, duration) ->
                val start = LocalDate.parse(date).atTime(20, 0).toInstant(ZoneOffset.UTC).toEpochMilli() + entryId
                StatisticsRecapSegment(entryId, null, date, start, start + duration, duration, "UTC")
            }
        }.sortedBy { it.startedAtEpochMillis }
        return StatisticsRecapActivity(
            segments = segments,
            entries = days.map { (entryId, _) ->
                StatisticsRecapEntry(
                    id = entryId,
                    type = EntryType.MANGA,
                    title = "Title $entryId",
                    cover = EntryCover(entryId, 0L, true, null, 0L),
                    genres = emptyList(),
                    sourceId = 0L,
                )
            },
            completions = emptyList(),
            finishedEntryIds = emptySet(),
            firstActiveDateByEntry = days.associate { (entryId, entryDays) -> entryId to entryDays.first().first },
            profileFirstActiveDate = firstActive,
        )
    }

    private companion object {
        const val HOUR = 3_600_000L
        val YEAR_2026 = StatisticsRecapPeriod.Year(2026)
        val CONTRIBUTIONS = listOf(
            EntryStatisticsContribution(
                type = EntryType.MANGA,
                accent = EntryStatisticsAccent.ROSE,
                consumedUnitLabel = MR.strings.statistics_chapters_read,
                consumedCountPlural = MR.plurals.statistics_chapters_read_count,
                itemPaceCarriesAcrossTitles = true,
                itemNumberLabel = MR.strings.statistics_recap_chapter_number,
                itemRangeLabel = MR.strings.statistics_recap_chapter_range,
            ),
        )
    }
}
