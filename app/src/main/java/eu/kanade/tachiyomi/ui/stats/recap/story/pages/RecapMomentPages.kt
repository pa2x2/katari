package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapItems
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.domain.statistics.recap.StatisticsRecapSegment
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** The first or last shown title of the period, at the moment it was opened. */
internal fun StatisticsRecapIndex.bookendPage(isFirst: Boolean): StatisticsRecapPage.Bookend? {
    val shown = segments.filter { isShown(it.entryId) }
    val segment = (if (isFirst) shown.firstOrNull() else shown.lastOrNull()) ?: return null
    return StatisticsRecapPage.Bookend(
        isFirst = isFirst,
        title = title(segment.entryId),
        item = segment.itemNumber?.let { number ->
            contribution(type(segment.entryId))?.let { StatisticsRecapItems.Single(it.itemNumberLabel, number) }
        },
        at = segment.localStart(),
    )
}

internal fun StatisticsRecapIndex.biggestDayPage(): StatisticsRecapPage.BiggestDay? {
    val (date, duration) = durationByDay.maxWithOrNull(compareBy({ it.value }, { it.key })) ?: return null
    val daySegments = segmentsByDay.getValue(date)
    val top = shownTitlesOf(daySegments).firstOrNull()
    val numbers = daySegments.filter { it.entryId == top?.entryId }.mapNotNull { it.itemNumber }
    val label = top?.let { contribution(it.type) }
    return StatisticsRecapPage.BiggestDay(
        date = date,
        durationMillis = duration,
        title = top,
        isMostlyOneTitle = top != null && top.durationMillis >= duration * MOSTLY_ONE_TITLE_SHARE,
        items = if (label == null || numbers.isEmpty()) {
            null
        } else if (numbers.min() == numbers.max()) {
            StatisticsRecapItems.Single(label.itemNumberLabel, numbers.min())
        } else {
            StatisticsRecapItems.Range(label.itemRangeLabel, numbers.min(), numbers.max())
        },
        from = daySegments.minOf { it.localStart() }.toLocalTime(),
        to = daySegments.maxOf { it.localEnd() }.toLocalTime(),
    )
}

private fun StatisticsRecapSegment.zone(): ZoneId = runCatching {
    ZoneId.of(timeZoneId)
}.getOrDefault(ZoneId.systemDefault())

private fun StatisticsRecapSegment.localStart(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(startedAtEpochMillis), zone())

private fun StatisticsRecapSegment.localEnd(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(endedAtEpochMillis), zone())

private const val MOSTLY_ONE_TITLE_SHARE = 0.8
