package eu.kanade.presentation.more.stats.layout

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.*

/** Dashboard sections. Cards keep their saved order inside a section; sections always render in this order. */
internal enum class StatisticsCardGroup(val label: StringResource) {
    ACTIVITY(MR.strings.statistics_activity),
    LIBRARY(MR.strings.statistics_library),
}

internal fun StatisticsCard.label(): StringResource = when (this) {
    StatisticsCard.SUMMARY -> MR.strings.statistics_summary
    StatisticsCard.ACTIVITY -> MR.strings.statistics_activity
    StatisticsCard.TOP_TITLES -> MR.strings.statistics_top_titles
    StatisticsCard.PATTERNS -> MR.strings.statistics_activity_patterns
    StatisticsCard.EARLIER -> MR.strings.statistics_lifetime
    StatisticsCard.PROGRESS -> MR.strings.statistics_progress
    StatisticsCard.MEDIA -> MR.strings.statistics_by_media
    StatisticsCard.INSIGHTS -> MR.strings.statistics_library_insights
}

internal val StatisticsCard.group: StatisticsCardGroup
    get() = when (this) {
        StatisticsCard.PROGRESS, StatisticsCard.MEDIA, StatisticsCard.INSIGHTS -> StatisticsCardGroup.LIBRARY
        else -> StatisticsCardGroup.ACTIVITY
    }

/** A short note for cards that only appear under some conditions, shown in the Customize sheet. */
internal fun StatisticsCard.availabilityHint(): StringResource? = when (this) {
    StatisticsCard.EARLIER -> MR.strings.statistics_lifetime_all_range_only
    else -> null
}

/** Preference key suffix for a tab's saved card layout. */
internal fun statisticsLayoutTab(type: EntryType?): String = type?.name ?: "overview"

internal fun statisticsCards(isOverview: Boolean): List<StatisticsCard> = StatisticsCard.entries.filter {
    if (isOverview) it != StatisticsCard.INSIGHTS else it != StatisticsCard.MEDIA
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
