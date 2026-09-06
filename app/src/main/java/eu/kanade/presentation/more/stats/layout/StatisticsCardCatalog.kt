package eu.kanade.presentation.more.stats.layout

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.i18n.MR

internal fun StatisticsCard.label(): StringResource = when (this) {
    StatisticsCard.SUMMARY -> MR.strings.statistics_summary
    StatisticsCard.ACTIVITY -> MR.strings.statistics_activity
    StatisticsCard.TOP_TITLES -> MR.strings.statistics_top_titles
    StatisticsCard.PATTERNS -> MR.strings.statistics_activity_patterns
    StatisticsCard.EARLIER -> MR.strings.statistics_earlier_activity
    StatisticsCard.PROGRESS -> MR.strings.statistics_progress
    StatisticsCard.MEDIA -> MR.strings.statistics_by_media
    StatisticsCard.INSIGHTS -> MR.strings.statistics_library_insights
}

internal val StatisticsCard.isCurrentLibrary: Boolean
    get() = this in setOf(StatisticsCard.PROGRESS, StatisticsCard.MEDIA, StatisticsCard.INSIGHTS)

internal fun statisticsCards(isOverview: Boolean): List<StatisticsCard> = StatisticsCard.entries.filter {
    if (isOverview) it != StatisticsCard.INSIGHTS else it != StatisticsCard.MEDIA
}
