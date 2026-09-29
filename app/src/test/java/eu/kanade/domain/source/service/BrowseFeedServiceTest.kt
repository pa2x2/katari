package eu.kanade.domain.source.service

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class BrowseFeedServiceTest {

    private val preferences = SourcePreferences(
        preferenceStore = BrowseFeedPreferenceStore(),
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    )
    private val service = BrowseFeedService(preferences)

    @Test
    fun `savePreset keeps timelines on rename and clears only feeds of a preset whose behavior changed`() {
        val updatedFeed = SourceFeed(id = "feed-1", sourceId = 1L, presetId = "preset")
        val untouchedFeed = SourceFeed(id = "feed-2", sourceId = 1L, presetId = "other")
        preferences.savedFeeds.set(listOf(updatedFeed, untouchedFeed))
        val updatedTimeline = SourceFeedTimeline(
            items = listOf(FeedItemRef(1L, EntryType.MANGA), FeedItemRef(2L, EntryType.MANGA)),
            nextPageKey = 3L,
        )
        val updatedAnchor = SourceFeedAnchor(item = FeedItemRef(2L, EntryType.MANGA), scrollOffset = 48)
        val untouchedTimeline = SourceFeedTimeline(
            items = listOf(FeedItemRef(4L, EntryType.MANGA), FeedItemRef(5L, EntryType.MANGA)),
            nextPageKey = 6L,
        )
        val untouchedAnchor = SourceFeedAnchor(item = FeedItemRef(5L, EntryType.MANGA), scrollOffset = 12)
        service.saveTimeline(updatedFeed.id, updatedTimeline)
        service.saveAnchor(updatedFeed.id, updatedAnchor)
        service.saveTimeline(untouchedFeed.id, untouchedTimeline)
        service.saveAnchor(untouchedFeed.id, untouchedAnchor)
        val preset = SourceFeedPreset(
            id = "preset",
            sourceId = 1L,
            name = "Tracked",
            listingMode = FeedListingMode.Search,
            query = "frieren",
        )
        service.savePreset(preset)
        service.savePreset(preset.copy(id = "other", name = "Other", query = "dungeon"))

        service.savePreset(preset.copy(name = "Renamed"))

        service.timelineSnapshot(updatedFeed.id) shouldBe updatedTimeline
        service.anchorSnapshot(updatedFeed.id) shouldBe updatedAnchor

        service.savePreset(preset.copy(name = "Renamed", query = "apothecary"))

        service.timelineSnapshot(updatedFeed.id) shouldBe SourceFeedTimeline()
        service.anchorSnapshot(updatedFeed.id) shouldBe SourceFeedAnchor()
        service.timelineSnapshot(untouchedFeed.id) shouldBe untouchedTimeline
        service.anchorSnapshot(untouchedFeed.id) shouldBe untouchedAnchor
    }

    @Test
    fun `restoring a removed feed puts back its place, saved position and selection`() {
        val first = SourceFeed(id = "feed-1", sourceId = 1L, presetId = "a")
        val removed = SourceFeed(id = "feed-2", sourceId = 1L, presetId = "b", title = "Mine")
        val last = SourceFeed(id = "feed-3", sourceId = 2L, presetId = "c")
        preferences.savedFeeds.set(listOf(first, removed, last))
        service.selectFeed(removed.id)
        val timeline = SourceFeedTimeline(items = listOf(FeedItemRef(7L, EntryType.MANGA)), nextPageKey = 2L)
        val anchor = SourceFeedAnchor(item = FeedItemRef(7L, EntryType.MANGA), scrollOffset = 30)
        service.saveTimeline(removed.id, timeline)
        service.saveAnchor(removed.id, anchor)

        val snapshot = service.removeFeed(removed.id)!!

        service.stateSnapshot().feeds shouldBe listOf(first, last)
        service.stateSnapshot().selectedFeedId shouldBe null
        service.timelineSnapshot(removed.id) shouldBe SourceFeedTimeline()

        service.restoreFeed(snapshot)

        service.stateSnapshot().feeds shouldBe listOf(first, removed, last)
        service.stateSnapshot().selectedFeedId shouldBe removed.id
        service.timelineSnapshot(removed.id) shouldBe timeline
        service.anchorSnapshot(removed.id) shouldBe anchor
    }
}
