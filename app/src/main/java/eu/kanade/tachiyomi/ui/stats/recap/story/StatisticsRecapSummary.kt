package eu.kanade.tachiyomi.ui.stats.recap.story

import androidx.compose.runtime.Immutable

/** The card a recap is shared as; [figures] holds at most four, most telling first. */
@Immutable
data class StatisticsRecapSummary(
    val durationMillis: Long,
    val titles: List<StatisticsRecapTitle>,
    val figures: List<SummaryFigure>,
)

@Immutable
sealed interface SummaryFigure {
    data class LongestRun(val days: Int) : SummaryFigure

    data class Finished(val count: Int) : SummaryFigure

    data class TopHour(val hour: Int) : SummaryFigure

    data class TopGenre(val name: String) : SummaryFigure

    data class ActiveDays(val days: Int) : SummaryFigure

    data class Consumed(val count: StatisticsRecapConsumedCount) : SummaryFigure
}
