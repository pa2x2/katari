package eu.kanade.domain.source.service

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterIdentity
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class BrowseFeedPresetMigrationTest {
    private val preferences = SourcePreferences(BrowseFeedPreferenceStore(), Json { ignoreUnknownKeys = true })
    private val service = BrowseFeedService(preferences)
    private val original = SourceFeedPreset(
        id = "preset",
        sourceId = 1,
        name = "Movies",
        listingMode = FeedListingMode.Search,
        filters = listOf(FilterStateNode.Select("Type", 1)),
    )
    private val migrated = listOf(FilterStateNode.Select("Format", 2, FilterIdentity("type", "movie")))

    @Test
    fun `identity migration preserves linked feed timeline and anchor`() {
        service.savePreset(original)
        preferences.savedFeeds.set(listOf(SourceFeed(id = "feed", sourceId = 1, presetId = original.id)))
        val timeline = SourceFeedTimeline(items = listOf(FeedItemRef(7, EntryType.ANIME)), nextPageKey = 4)
        val anchor = SourceFeedAnchor(item = FeedItemRef(7, EntryType.ANIME), scrollOffset = 18)
        service.saveTimeline("feed", timeline)
        service.saveAnchor("feed", anchor)
        service.migratePresetFilters(original, migrated)
        service.stateSnapshot().presets.single().filters shouldBe migrated
        service.timelineSnapshot("feed") shouldBe timeline
        service.anchorSnapshot("feed") shouldBe anchor
    }

    @Test
    fun `a stale migration cannot overwrite an edited or deleted preset`() {
        service.savePreset(original)
        val edited = original.copy(query = "new query")
        BrowseFeedService(preferences).savePreset(edited)
        service.migratePresetFilters(original, migrated)
        service.stateSnapshot().presets.single() shouldBe edited
        service.removePreset(original.id)
        service.migratePresetFilters(original, migrated)
        service.stateSnapshot().presets shouldBe emptyList()
    }
}
