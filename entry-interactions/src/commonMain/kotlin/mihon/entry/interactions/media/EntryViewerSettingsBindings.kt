package mihon.entry.interactions.media

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.settings.DefaultViewerSettingBinder
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import tachiyomi.domain.entry.repository.EntryRepository

/**
 * The viewer settings feature and the binder that resolves its values, kept apart from [EntryMediaBindings] so that a
 * platform which doesn't host its entry types' viewers still answers which viewer settings they have.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryViewerSettingsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun viewerSettingBinder(
        overrideRepository: ViewerSettingOverrideRepository,
        @AppCoroutineScope scope: CoroutineScope,
    ): ViewerSettingBinder = DefaultViewerSettingBinder(
        overrideRepository = overrideRepository,
        scope = scope + Dispatchers.Default,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun viewerSettingsFeature(
        viewerSettingsProviders: Map<EntryType, EntryViewerSettingsProvider>,
        migrationProviders: Map<EntryType, EntryMigrationProvider>,
        interactions: EntryInteractions,
        projectionResolver: EntryViewerSettingsScreenProjectionResolver,
        overrideRepository: ViewerSettingOverrideRepository,
        entryRepository: () -> EntryRepository,
    ): EntryViewerSettingsFeature = DefaultEntryViewerSettingsFeature(
        applicableTypes = viewerSettingsProviders.keys,
        migrationTypes = viewerSettingsProviders.keys intersect migrationProviders.keys,
        interaction = interactions.viewerSettings,
        projectionResolver = projectionResolver,
        overrideRepository = overrideRepository,
        legacyMangaViewerFlagsReset = EntryLegacyMangaViewerFlagsReset {
            entryRepository().resetViewerFlags()
        },
        migrationStore = EntryViewerFlagsMigrationStore { entryId, profileId, viewerFlags ->
            val repository = entryRepository()
            repository.getEntryById(entryId, profileId)
                ?.let { current ->
                    repository.update(
                        current.copy(viewerFlags = viewerFlags),
                        profileId,
                    )
                }
                ?: false
        },
    )
}
