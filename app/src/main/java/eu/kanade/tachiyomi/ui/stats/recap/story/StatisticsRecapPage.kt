package eu.kanade.tachiyomi.ui.stats.recap.story

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/** One page of a recap story; each holds the facts it shows, already chosen and ordered. */
@Immutable
sealed interface StatisticsRecapPage {

    /** The cover a page's colours come from; null for pages about no title in particular. */
    val featured: StatisticsRecapTitle?

    data class Opening(val covers: List<StatisticsRecapTitle>) : StatisticsRecapPage {
        override val featured get() = covers.firstOrNull()
    }

    data class TotalTime(val durationMillis: Long) : StatisticsRecapPage {
        override val featured: StatisticsRecapTitle? get() = null
    }

    /**
     * @param isFirst true for the first activity of the period, false for the last.
     * @param at local date and time in the zone the activity was recorded in.
     */
    data class Bookend(
        val isFirst: Boolean,
        val title: StatisticsRecapTitle,
        val item: StatisticsRecapItems.Single?,
        val at: LocalDateTime,
    ) : StatisticsRecapPage {
        override val featured get() = title
    }

    data class TopTitle(
        val title: StatisticsRecapTitle,
        val consumed: StatisticsRecapConsumedCount?,
        val moment: TopTitleMoment,
    ) : StatisticsRecapPage {
        override val featured get() = title
    }

    /** @param sharePercent of the period's time taken by the whole top five, #1 included. */
    data class TopFive(val titles: List<StatisticsRecapTitle>, val sharePercent: Int) : StatisticsRecapPage {
        override val featured get() = titles.firstOrNull()
    }

    /** @param months every month of the period with its top title; null where nothing shown was timed. */
    data class MonthTitles(
        val months: List<Pair<YearMonth, StatisticsRecapTitle?>>,
        val longestHold: MonthHold?,
    ) : StatisticsRecapPage {
        override val featured get() = longestHold?.title ?: months.firstNotNullOfOrNull { it.second }
    }

    data class MonthTime(
        val months: List<Pair<YearMonth, Long>>,
        val biggest: YearMonth,
        val biggestTitle: StatisticsRecapTitle?,
    ) : StatisticsRecapPage {
        override val featured get() = biggestTitle
        val biggestDurationMillis: Long get() = months.first { it.first == biggest }.second
    }

    /**
     * @param title the title with the most time that day; null when every title of the day is hidden.
     * @param isMostlyOneTitle whether [title] took nearly all of the day.
     */
    data class BiggestDay(
        val date: LocalDate,
        val durationMillis: Long,
        val title: StatisticsRecapTitle?,
        val isMostlyOneTitle: Boolean,
        val items: StatisticsRecapItems?,
        val from: LocalTime,
        val to: LocalTime,
    ) : StatisticsRecapPage {
        override val featured get() = title
    }

    /**
     * @param hourlyDurationMillis time per local clock hour, from midnight.
     * @param windowStartHour first hour of the four-hour span holding the most time; it may wrap past midnight.
     */
    data class Hours(
        val hourlyDurationMillis: List<Long>,
        val topHour: Int,
        val windowStartHour: Int,
        val windowPercent: Int,
        val busiestWeekday: DayOfWeek,
    ) : StatisticsRecapPage {
        override val featured: StatisticsRecapTitle? get() = null
    }

    /** @param activeDates every day of the period counted toward streaks. */
    data class LongestRun(
        val run: ClosedRange<LocalDate>,
        val activeDays: Int,
        val activeDates: Set<LocalDate>,
        val periodStart: LocalDate,
        val periodEnd: LocalDate,
    ) : StatisticsRecapPage {
        override val featured: StatisticsRecapTitle? get() = null
    }

    /** @param count every title finished, hidden ones included; [titles] are the ones that may be shown. */
    data class Finished(
        val count: Int,
        val titles: List<StatisticsRecapTitle>,
        val consumed: List<StatisticsRecapConsumedCount>,
    ) : StatisticsRecapPage {
        override val featured get() = titles.firstOrNull()
    }

    data class NewTitles(val count: Int, val standout: StatisticsRecapTitle?) : StatisticsRecapPage {
        override val featured get() = standout
    }

    /** @param ledMonths months led by a type other than the one with the most time. */
    data class Types(
        val shares: List<TypeShare>,
        val ledMonths: Pair<EntryType, List<YearMonth>>?,
    ) : StatisticsRecapPage {
        override val featured: StatisticsRecapTitle? get() = null
    }

    /** @param topGenreTitles how many of the [topTitleCount] titles with the most time have the top genre. */
    data class Genres(
        val genres: List<GenreShare>,
        val topGenreTitles: Int,
        val topTitleCount: Int,
    ) : StatisticsRecapPage {
        override val featured: StatisticsRecapTitle? get() = null
    }

    /** @param sameTopTitle the #1 title when it was #1 of the previous period too. */
    data class Comparison(
        val previous: ComparedPeriod,
        val changePercent: Int,
        val sameTopTitle: StatisticsRecapTitle?,
    ) : StatisticsRecapPage {
        override val featured get() = sameTopTitle
    }

    /** A month's opening: its time against the month before and where most of it went. */
    data class MonthTotal(
        val month: YearMonth,
        val durationMillis: Long,
        val previousDurationMillis: Long?,
        val topTitle: StatisticsRecapTitle?,
    ) : StatisticsRecapPage {
        override val featured get() = topTitle
    }

    data class MonthHighlights(
        val titles: List<StatisticsRecapTitle>,
        val biggestDay: BiggestDay,
    ) : StatisticsRecapPage {
        override val featured get() = titles.firstOrNull()
    }

    data class Summary(val summary: StatisticsRecapSummary) : StatisticsRecapPage {
        override val featured get() = summary.titles.firstOrNull()
    }
}

/** What the #1 page tells besides the totals. */
@Immutable
data class TopTitleMoment(
    /** Null when the title was already going before the period began. */
    val startedOn: LocalDate?,
    /** Null unless every item was finished in the period. */
    val finishedOn: LocalDate?,
    val activeDays: Int,
)

/** A run of consecutive months with the same top title. */
@Immutable
data class MonthHold(val title: StatisticsRecapTitle, val from: YearMonth, val to: YearMonth)

@Immutable
data class TypeShare(val type: EntryType, val durationMillis: Long, val percent: Int)

/** @param percent of the period's time spent in titles with this genre. */
@Immutable
data class GenreShare(val name: String, val percent: Int)

/** The period a comparison is made with, as a year or a month. */
@Immutable
sealed interface ComparedPeriod {
    data class Year(val year: Int) : ComparedPeriod

    data class Month(val month: YearMonth) : ComparedPeriod
}
