package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsLabelCount
import eu.kanade.presentation.more.stats.data.StatsLibraryInsights
import eu.kanade.presentation.more.stats.data.StatsProgress
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.library.model.LibraryItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

internal fun buildLibraryProgress(items: List<LibraryItem>): StatsProgress? {
    val supportedItems = items.filter(LibraryItem::hasProgressSummary)
    if (supportedItems.isEmpty()) return null
    var notStarted = 0
    var inProgress = 0
    var caughtUp = 0
    var completed = 0
    supportedItems.forEach { item ->
        val consumed = checkNotNull(item.consumedCount)
        val total = checkNotNull(item.totalCount)
        when {
            item.hasStarted != true -> notStarted += 1
            total > 0L && consumed >= total && item.entry.status == EntryStatus.COMPLETED -> completed += 1
            total > 0L && consumed >= total -> caughtUp += 1
            else -> inProgress += 1
        }
    }
    return StatsProgress(
        notStarted = notStarted,
        inProgress = inProgress,
        caughtUp = caughtUp,
        completed = completed,
        unavailable = items.size - supportedItems.size,
    )
}

internal fun buildLibraryInsights(
    items: List<LibraryItem>,
    today: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault(),
): StatsLibraryInsights {
    val genres = items
        .flatMap { item ->
            item.entry.genre.orEmpty()
                .map { genre -> genre.trim().replace(WHITESPACE_REGEX, " ") }
                .filter(String::isNotEmpty)
                .distinctBy { it.lowercase(Locale.ROOT) }
        }
        .groupBy { it.lowercase(Locale.ROOT) }
        .values
        .map { spellings -> StatsLabelCount(spellings.first(), spellings.size) }
        .sortedWith(
            compareByDescending<StatsLabelCount> {
                it.count
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
        )
    val sources = items
        .groupBy { it.sourceName }
        .map { (name, sourceItems) -> StatsLabelCount(name, sourceItems.size) }
        .sortedWith(
            compareByDescending<StatsLabelCount> {
                it.count
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
        )
    val topSources = sources.take(TOP_SOURCE_COUNT)
    val addedByMonth = IntArray(today.monthValue)
    items.forEach { item ->
        val added = Instant.ofEpochMilli(item.dateAdded).atZone(zoneId).toLocalDate()
        if (item.dateAdded > 0L && added.year == today.year && !added.isAfter(today)) {
            addedByMonth[added.monthValue - 1] += 1
        }
    }
    val unconsumed = items.mapNotNull { it.unconsumedCount }.filter { it > 0L }
    return StatsLibraryInsights(
        topGenres = genres.take(TOP_GENRE_COUNT),
        categoryCount = items.flatMap(LibraryItem::categories).distinct().size,
        unconsumedCount = unconsumed.sum(),
        titlesWithUnconsumed = unconsumed.size,
        downloadedCount = items.sumOf { it.downloadCount.toLong() },
        statusCounts = items.groupingBy { it.entry.status }.eachCount(),
        topSources = topSources,
        otherSourcesTitleCount = sources.drop(TOP_SOURCE_COUNT).sumOf { it.count },
        otherSourceCount = (sources.size - TOP_SOURCE_COUNT).coerceAtLeast(0),
        addedByMonth = addedByMonth.toList(),
        addedYear = today.year,
    )
}

private const val TOP_GENRE_COUNT = 5
private const val TOP_SOURCE_COUNT = 3

private val WHITESPACE_REGEX = Regex("\\s+")
