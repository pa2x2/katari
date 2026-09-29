package eu.kanade.domain.source.service

import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterIdentity
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.domain.source.model.SourceFeedPreset
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
