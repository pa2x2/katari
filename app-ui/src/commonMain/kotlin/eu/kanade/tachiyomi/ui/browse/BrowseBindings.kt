package eu.kanade.tachiyomi.ui.browse

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.interactor.GetEnabledCatalogSources
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.domain.source.repository.SourceRepository

/** The source services browsing reads that need an explicit constructor call. */
@ContributesTo(AppScope::class)
@BindingContainer
object BrowseBindings {
    // Both take the per-profile source preferences as an optional parameter for tests; production passes them.
    @Provides
    fun getEnabledCatalogSources(
        repository: SourceRepository,
        preferences: SourcePreferences,
        profilePreferences: ProfileSourcePreferences,
    ): GetEnabledCatalogSources = GetEnabledCatalogSources(repository, preferences, profilePreferences)

    @Provides
    @SingleIn(AppScope::class)
    fun browseFeedService(
        preferences: SourcePreferences,
        profilePreferences: ProfileSourcePreferences,
    ): BrowseFeedService = BrowseFeedService(preferences, profilePreferences)
}
