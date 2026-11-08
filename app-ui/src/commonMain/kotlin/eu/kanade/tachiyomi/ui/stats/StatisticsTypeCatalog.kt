package eu.kanade.tachiyomi.ui.stats

import app.katari.entry.interactions.presentation.EntryTypePresentationFeature
import app.katari.entry.interactions.presentation.EntryTypePresentationResult
import app.katari.entry.interactions.statistics.EntryStatisticsFeature
import eu.kanade.presentation.more.stats.data.StatsType

fun buildStatisticsTypes(
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
        consumedUnitName = contribution.consumedUnitName,
        timePerItemLabel = contribution.timePerItemLabel,
    )
}
