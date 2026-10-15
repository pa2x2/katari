package eu.kanade.tachiyomi.ui.browse

import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.tachiyomi.ui.browse.feed.CatalogFeedItemSource
import mihon.core.common.CustomPreferences
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.domain.source.service.SourceManager

/** Graph services the browse composables and the source preferences fragment read outside a view model. */
interface BrowseGraph {
    val browseFeedService: BrowseFeedService
    val catalogFeedItemSourceFactory: CatalogFeedItemSource.Factory
    val customPreferences: CustomPreferences
    val entryImmersiveFeature: EntryImmersiveFeature
    val entryMigrationFeature: EntryMigrationFeature
    val entryOpenFeature: EntryOpenFeature
    val entrySourceSettingsFeature: EntrySourceSettingsFeature
    val profileManager: ProfileManager
    val profileSourcePreferences: ProfileSourcePreferences
    val sourceManager: SourceManager
}
