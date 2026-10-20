package eu.kanade.tachiyomi.ui.browse.feed.timeline

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.domain.source.model.SourceFeedNewItems
import eu.kanade.domain.source.model.SourceFeedTimeline

/*
 * How pages of a chronological listing join a saved timeline. The listing is newest first, and new uploads push
 * everything down while a feed catches up, so a page loaded later can repeat items an earlier page already brought.
 * Items already in the timeline are therefore skipped wherever they appear, never added twice.
 */

internal data class TimelineUpdate(
    val timeline: SourceFeedTimeline,
    val addedCount: Int,
    /** The gap left open after this page, or null when there is nothing more to load into it. */
    val gap: SourceFeedGap?,
)

/**
 * The first page of a refresh. Its items go above the timeline; when none of them is already there, the rest of the
 * listing down to the timeline is still missing, so a gap opens below them.
 */
internal fun SourceFeedTimeline.withRefreshPage(pageRefs: List<FeedItemRef>, nextPageKey: Long?): TimelineUpdate {
    val existing = resolvedItems()
    if (existing.isEmpty()) {
        return TimelineUpdate(
            SourceFeedTimeline.fromItems(pageRefs.distinct(), nextPageKey),
            addedCount = 0,
            gap = null,
        )
    }

    val existingSet = existing.toHashSet()
    val overlapIndex = pageRefs.indexOfFirst { it in existingSet }
    val newRefs = (if (overlapIndex >= 0) pageRefs.take(overlapIndex) else pageRefs).distinct()
    if (newRefs.isEmpty()) return TimelineUpdate(this, addedCount = 0, gap = null)

    val gap = if (overlapIndex < 0 && nextPageKey != null) SourceFeedGap(newRefs.last(), nextPageKey) else null
    val updated = copy(
        items = newRefs + existing,
        legacyMangaIds = emptyList(),
        gaps = listOfNotNull(gap) + gaps,
        newItems = newItems.afterArrivalAbove(existing.first()),
    )
    return TimelineUpdate(updated, addedCount = newRefs.size, gap = gap)
}

/**
 * A page loaded into [gap]. Its items go below the gap's item until one of them is already further down, which closes
 * the gap; so does the end of the listing.
 */
internal fun SourceFeedTimeline.withGapPage(
    gap: SourceFeedGap,
    pageRefs: List<FeedItemRef>,
    nextPageKey: Long?,
): TimelineUpdate {
    val existing = resolvedItems()
    val gapIndex = existing.indexOf(gap.after)
    if (gapIndex < 0 || gap !in gaps) return TimelineUpdate(withoutGap(gap), addedCount = 0, gap = null)

    val below = existing.subList(gapIndex + 1, existing.size).toHashSet()
    val overlapIndex = pageRefs.indexOfFirst { it in below }
    val existingSet = existing.toHashSet()
    val newRefs = (if (overlapIndex >= 0) pageRefs.take(overlapIndex) else pageRefs)
        .filterNot { it in existingSet }
        .distinct()
    val remaining = if (overlapIndex >= 0 || nextPageKey == null) {
        null
    } else {
        SourceFeedGap(newRefs.lastOrNull() ?: gap.after, nextPageKey)
    }

    val updated = copy(
        items = existing.take(gapIndex + 1) + newRefs + existing.drop(gapIndex + 1),
        legacyMangaIds = emptyList(),
        gaps = gaps.mapNotNull { if (it == gap) remaining else it },
    )
    return TimelineUpdate(updated, addedCount = newRefs.size, gap = remaining)
}

internal fun SourceFeedTimeline.withoutGap(gap: SourceFeedGap): SourceFeedTimeline {
    return copy(gaps = gaps - gap)
}

internal fun SourceFeedTimeline.withNewItemsViewed(): SourceFeedTimeline {
    val newItems = newItems?.takeUnless { it.viewed } ?: return this
    return copy(newItems = newItems.copy(viewed = true))
}

/**
 * The timeline with only [kept] of its items, in their order. A gap whose item is dropped moves up to the nearest
 * remaining item above it, and the new-items boundary moves down to the nearest remaining item below it.
 */
internal fun SourceFeedTimeline.retainingItems(kept: List<FeedItemRef>): SourceFeedTimeline {
    val existing = resolvedItems()
    val keptSet = kept.toHashSet()
    val gaps = gaps
        .mapNotNull { gap ->
            val index = existing.indexOf(gap.after)
            if (index < 0) return@mapNotNull null
            existing.subList(0, index + 1).lastOrNull { it in keptSet }?.let { gap.copy(after = it) }
        }
        .distinctBy { it.after }
    val newItems = newItems?.let { marker ->
        val index = existing.indexOf(marker.firstSeen)
        if (index < 0) return@let null
        existing.drop(index).firstOrNull { it in keptSet }?.let { marker.copy(firstSeen = it) }
    }
    return copy(items = kept, legacyMangaIds = emptyList(), gaps = gaps, newItems = newItems)
}

private fun SourceFeedNewItems?.afterArrivalAbove(previousTop: FeedItemRef): SourceFeedNewItems {
    return if (this == null || viewed) SourceFeedNewItems(firstSeen = previousTop) else this
}
