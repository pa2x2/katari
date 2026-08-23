package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsLibraryCoverage
import eu.kanade.presentation.more.stats.data.StatsLibraryInsights
import eu.kanade.presentation.more.stats.data.StatsOfflineCoverage
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsTrackingCoverage
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.library.model.LibraryItem
import java.util.Locale

internal fun buildLibraryProgress(items: List<LibraryItem>): StatsProgress? {
    if (items.any { !it.hasProgressSummary }) return null
    var notStarted = 0
    var inProgress = 0
    var caughtUp = 0
    var completed = 0
    items.forEach { item ->
        val consumed = checkNotNull(item.consumedCount)
        val total = checkNotNull(item.totalCount)
        when {
            consumed <= 0L -> notStarted += 1
            total > 0L && consumed >= total && item.entry.status == EntryStatus.COMPLETED -> completed += 1
            total > 0L && consumed >= total -> caughtUp += 1
            else -> inProgress += 1
        }
    }
    return StatsProgress(notStarted, inProgress, caughtUp, completed)
}

internal fun buildLibraryCoverage(
    items: List<LibraryItem>,
    types: List<EntryType>,
    downloadApplicable: (EntryType) -> Boolean,
    downloadCount: (LibraryItem) -> Int,
    trackingApplicable: (EntryType) -> Boolean,
    connectedTrackingTypes: Set<EntryType>,
    trackedEntryIds: Set<Long>,
): Map<EntryType, StatsLibraryCoverage> = types.associateWith { type ->
    val typeItems = items.filter { it.entry.type == type }
    val offline = if (downloadApplicable(type)) {
        var partly = 0
        var fully = 0
        typeItems.forEach { item ->
            val downloaded = downloadCount(item)
            if (downloaded <= 0) return@forEach
            val total = item.totalCount
            if (!item.isMerged && total != null && total > 0L && downloaded.toLong() >= total) {
                fully += 1
            } else {
                partly += 1
            }
        }
        StatsOfflineCoverage(partlyOfflineTitles = partly, fullyOfflineTitles = fully)
    } else {
        null
    }
    val tracking = when {
        !trackingApplicable(type) -> StatsTrackingCoverage.Unsupported
        type !in connectedTrackingTypes -> StatsTrackingCoverage.NotConnected
        else -> StatsTrackingCoverage.Connected(
            trackedTitles = typeItems.count { item ->
                item.memberEntries.any { it.id in trackedEntryIds }
            },
            totalTitles = typeItems.size,
        )
    }
    StatsLibraryCoverage(offline = offline, tracking = tracking)
}

internal fun buildLibraryInsights(items: List<LibraryItem>): StatsLibraryInsights {
    val genres = items
        .flatMap { it.entry.genre.orEmpty() }
        .map { genre -> genre.trim().replace(WHITESPACE_REGEX, " ") }
        .filter(String::isNotEmpty)
        .groupBy { it.lowercase(Locale.ROOT) }
        .values
    val topGenre = genres
        .sortedWith(
            compareByDescending<List<String>> { it.size }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first() },
        )
        .firstOrNull()
        ?.first()
    return StatsLibraryInsights(
        topGenre = topGenre,
        categoryCount = items.flatMap(LibraryItem::categories).distinct().size,
        sourceCount = items.flatMap { it.sourceIds }.distinct().size,
    )
}

private val WHITESPACE_REGEX = Regex("\\s+")
