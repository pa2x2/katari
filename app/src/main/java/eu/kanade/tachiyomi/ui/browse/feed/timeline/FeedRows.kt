package eu.kanade.tachiyomi.ui.browse.feed.timeline

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.domain.source.model.SourceFeedNewItems

/** One row of a feed as drawn: an item, a gap below the item it follows, or the line above the first seen item. */
internal sealed interface FeedRow {
    val key: String

    data class Item(val ref: FeedItemRef) : FeedRow {
        override val key: String = ref.rowKey()
    }

    data class Gap(val gap: SourceFeedGap) : FeedRow {
        override val key: String = "feed-gap-${gap.after.type.name}-${gap.after.id}"
    }

    data object SeenDivider : FeedRow {
        override val key: String = "feed-seen-divider"
    }
}

internal fun FeedItemRef.rowKey(): String = "feed-${type.name}-$id"

/** The divider is left out at the top, where nothing is new, and right below a gap, which already marks the boundary. */
internal fun feedRows(
    itemRefs: List<FeedItemRef>,
    gaps: List<SourceFeedGap>,
    newItems: SourceFeedNewItems?,
): List<FeedRow> {
    val gapsByItem = gaps.associateBy(SourceFeedGap::after)
    return buildList(itemRefs.size + gaps.size + 1) {
        itemRefs.forEachIndexed { index, ref ->
            if (index > 0 && ref == newItems?.firstSeen && lastOrNull() !is FeedRow.Gap) add(FeedRow.SeenDivider)
            add(FeedRow.Item(ref))
            gapsByItem[ref]?.let { add(FeedRow.Gap(it)) }
        }
    }
}
