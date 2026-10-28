package eu.kanade.tachiyomi.ui.browse

import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.tachiyomi.ui.browse.feed.CatalogFeedItemSource
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesDestination
import mihon.core.common.CustomPreferences
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager

/** Graph services the browse composables and their long-press settings read outside a view model. */
interface BrowseGraph {
    val browseFeedService: BrowseFeedService
    val catalogFeedItemSourceFactory: CatalogFeedItemSource.Factory
    val customPreferences: CustomPreferences
    val entryImmersiveFeature: EntryImmersiveFeature
    val entryMigrationFeature: EntryMigrationFeature
    val entryOpenFeature: EntryOpenFeature
    val profileManager: ProfileManager
    val profileSourcePreferences: ProfileSourcePreferences
    val sourceManager: SourceManager
    val sourceRepository: SourceRepository
    val sourcePreferencesDestination: SourcePreferencesDestination
}
