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
}
