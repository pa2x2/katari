package eu.kanade.tachiyomi.ui.browse.feed.timeline

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedGap

/** One row of a feed as drawn: an item, or a gap below the item it follows. */
internal sealed interface FeedRow {
    val key: String

    data class Item(val ref: FeedItemRef) : FeedRow {
        override val key: String = ref.rowKey()
    }

    data class Gap(val gap: SourceFeedGap) : FeedRow {
        override val key: String = "feed-gap-${gap.after.type.name}-${gap.after.id}"
    }
}

internal fun FeedItemRef.rowKey(): String = "feed-${type.name}-$id"

internal fun feedRows(itemRefs: List<FeedItemRef>, gaps: List<SourceFeedGap>): List<FeedRow> {
    val gapsByItem = gaps.associateBy(SourceFeedGap::after)
    return buildList(itemRefs.size + gaps.size) {
        itemRefs.forEach { ref ->
            add(FeedRow.Item(ref))
            gapsByItem[ref]?.let { add(FeedRow.Gap(it)) }
        }
    }
}
