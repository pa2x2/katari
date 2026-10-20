package eu.kanade.tachiyomi.ui.browse.feed.timeline

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.domain.source.model.SourceFeedNewItems
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FeedTimelineUpdatesTest {

    @Test
    fun `items pushed down by new uploads are not added twice when a gap loads`() {
        val saved = SourceFeedTimeline.fromItems(refs(100, 101), nextPageKey = 9L)

        val refreshed = saved.withRefreshPage(refs(1, 2), nextPageKey = 2L)
        refreshed.gap shouldBe SourceFeedGap(ref(2), nextPageKey = 2L)

        // Uploads since page 1 shifted item 2 onto page 2; item 100 is where the saved timeline resumes.
        val filled = refreshed.timeline.withGapPage(refreshed.gap!!, refs(2, 3, 100, 101), nextPageKey = 3L)

        filled.gap shouldBe null
        filled.addedCount shouldBe 1
        filled.timeline.items shouldBe refs(1, 2, 3, 100, 101)
        filled.timeline.gaps shouldBe emptyList()
        filled.timeline.nextPageKey shouldBe 9L
    }

    @Test
    fun `refreshes the reader has not looked at keep adding to the new items`() {
        val saved = SourceFeedTimeline.fromItems(refs(100), nextPageKey = null)

        val first = saved.withRefreshPage(refs(2, 100), nextPageKey = null).timeline
        val second = first.withRefreshPage(refs(1, 2), nextPageKey = null).timeline
        second.newItems shouldBe SourceFeedNewItems(firstSeen = ref(100))

        val afterLooking = second.withNewItemsViewed().withRefreshPage(refs(0, 1), nextPageKey = null).timeline
        afterLooking.newItems shouldBe SourceFeedNewItems(firstSeen = ref(1))
    }

    @Test
    fun `dropping items keeps the gap and the new-items boundary between the remaining ones`() {
        val timeline = SourceFeedTimeline.fromItems(
            items = refs(1, 2, 3, 4),
            nextPageKey = null,
            gaps = listOf(SourceFeedGap(ref(2), nextPageKey = 5L)),
            newItems = SourceFeedNewItems(firstSeen = ref(3)),
        )

        val retained = timeline.retainingItems(refs(1, 4))

        retained.gaps shouldBe listOf(SourceFeedGap(ref(1), nextPageKey = 5L))
        retained.newItems shouldBe SourceFeedNewItems(firstSeen = ref(4))
    }

    private fun ref(id: Long) = FeedItemRef(id, EntryType.MANGA)

    private fun refs(vararg ids: Long) = ids.map(::ref)
}
