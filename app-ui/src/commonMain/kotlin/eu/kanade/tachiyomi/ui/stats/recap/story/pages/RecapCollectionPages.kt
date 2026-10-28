package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import java.time.LocalDate

internal fun StatisticsRecapIndex.finishedPage(): StatisticsRecapPage.Finished? {
    val finished = activity.finishedEntryIds.takeIf { it.isNotEmpty() } ?: return null
    return StatisticsRecapPage.Finished(
        count = finished.size,
        titles = shownTitles.filter { it.entryId in finished },
        consumed = consumedCounts(),
    )
}

/**
 * Titles first opened in the period. Only told when the profile was active before the period began; otherwise
 * every title would look new.
 */
internal fun StatisticsRecapIndex.newTitlesPage(): StatisticsRecapPage.NewTitles? {
    val firstActive = activity.profileFirstActiveDate?.let(LocalDate::parse) ?: return null
    if (!firstActive.isBefore(period.start)) return null
    val newTitles = titles.filter { title ->
        activity.firstActiveDateByEntry[title.entryId]?.let { !LocalDate.parse(it).isBefore(period.start) } == true
    }.takeIf { it.isNotEmpty() } ?: return null
    return StatisticsRecapPage.NewTitles(
        count = newTitles.size,
        standout = newTitles.firstOrNull { isShown(it.entryId) },
    )
}
