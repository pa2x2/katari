package eu.kanade.presentation.more.stats.layout

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.*

/**
 * Dashboard bands, by the time their numbers cover: the chosen period, the last 12 months, and the library as it is
 * now. Cards keep their saved order inside a band; bands always render in this order.
 */
internal enum class StatisticsCardGroup(val label: StringResource) {
    PERIOD(MR.strings.statistics_activity),
    YEAR(MR.strings.statistics_last_12_months),
    LIBRARY(MR.strings.statistics_library),
}

internal fun StatisticsCard.label(): StringResource = when (this) {
    StatisticsCard.ACTIVITY -> MR.strings.statistics_time_spent
    StatisticsCard.TOP_TITLES -> MR.strings.statistics_top_titles
    StatisticsCard.PATTERNS -> MR.strings.statistics_when_you_read
    StatisticsCard.EARLIER -> MR.strings.statistics_lifetime
    StatisticsCard.CALENDAR -> MR.strings.statistics_reading_calendar
    StatisticsCard.LIBRARY -> MR.strings.statistics_progress
    StatisticsCard.INSIGHTS -> MR.strings.statistics_library_insights
}

internal val StatisticsCard.group: StatisticsCardGroup
    get() = when (this) {
        StatisticsCard.ACTIVITY,
        StatisticsCard.TOP_TITLES,
        StatisticsCard.PATTERNS,
        StatisticsCard.EARLIER,
        -> StatisticsCardGroup.PERIOD
        StatisticsCard.CALENDAR -> StatisticsCardGroup.YEAR
        StatisticsCard.LIBRARY, StatisticsCard.INSIGHTS -> StatisticsCardGroup.LIBRARY
    }

/**
 * Whether the card takes the dashboard's full width when it shows two columns of cards. The period's chart and tiles
 * head the page, and the reading calendar draws time along its width, so a column would show less of it; the other
 * cards are lists, bars and figures that read the same at a column's width.
 */
internal val StatisticsCard.spansColumns: Boolean
    get() = when (this) {
        StatisticsCard.ACTIVITY, StatisticsCard.CALENDAR -> true
        StatisticsCard.TOP_TITLES,
        StatisticsCard.PATTERNS,
        StatisticsCard.EARLIER,
        StatisticsCard.LIBRARY,
        StatisticsCard.INSIGHTS,
        -> false
    }

/** A short note for cards that only appear under some conditions, shown in the Customize sheet. */
internal fun StatisticsCard.availabilityHint(): StringResource? = when (this) {
    StatisticsCard.EARLIER -> MR.strings.statistics_lifetime_all_range_only
    else -> null
}

/** Preference key suffix for a tab's saved card layout. */
internal fun statisticsLayoutTab(type: EntryType?): String = type?.name ?: "overview"

/** Library insights describe one type, so only a type's tab has them. */
internal fun statisticsCards(isOverview: Boolean): List<StatisticsCard> = StatisticsCard.entries.filter {
    !isOverview || it != StatisticsCard.INSIGHTS
}

internal fun StatisticsCard.isShownFor(range: StatsRange): Boolean =
    this != StatisticsCard.EARLIER || range == StatsRange.ALL

/** Visible cards by section, in section order, skipping empty sections. */
internal fun StatisticsCardLayout.visibleSections(
    isOverview: Boolean,
    range: StatsRange,
): List<Pair<StatisticsCardGroup, List<StatisticsCard>>> {
    val available = statisticsCards(isOverview)
    return StatisticsCardGroup.entries.mapNotNull { group ->
        order
            .filter { it in available && it !in hidden && it.group == group && it.isShownFor(range) }
            .takeIf { it.isNotEmpty() }
            ?.let { group to it }
    }
}
