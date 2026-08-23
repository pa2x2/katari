package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsType
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.presentation.EntryTypePresentationResult
import mihon.entry.interactions.statistics.EntryStatisticsFeature

internal fun buildStatisticsTypes(
    statisticsFeature: EntryStatisticsFeature,
    presentationFeature: EntryTypePresentationFeature,
): List<StatsType> = statisticsFeature.contributions.map { contribution ->
    val presentation = checkNotNull(
        presentationFeature.presentation(contribution.type) as? EntryTypePresentationResult.Contributed,
    ) { "Statistics contribution ${contribution.type} requires contributed type presentation" }.presentation
    StatsType(
        type = contribution.type,
        displayName = presentation.displayNameLabel,
        icon = presentation.badgeIcon,
        accent = contribution.accent,
        consumedUnitLabel = contribution.consumedUnitLabel,
    )
}
