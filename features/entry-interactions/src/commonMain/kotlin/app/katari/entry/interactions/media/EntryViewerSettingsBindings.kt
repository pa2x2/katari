package app.katari.entry.interactions.media

import app.katari.core.metro.AppCoroutineScope
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.settings.DefaultViewerSettingBinder
import app.katari.entry.interactions.state.EntryMigrationProvider
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.entry.viewer.settings.ViewerSettingOverrideRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
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
        openProcessors: Map<EntryType, EntryOpenProcessor>,
        migrationProviders: Map<EntryType, EntryMigrationProvider>,
        interactions: EntryInteractions,
        projectionResolver: EntryViewerSettingsScreenProjectionResolver,
        overrideRepository: ViewerSettingOverrideRepository,
        entryRepository: () -> EntryRepository,
    ): EntryViewerSettingsFeature = DefaultEntryViewerSettingsFeature(
        applicableTypes = viewerSettingsProviders.keys,
        // A type's viewer is what opens its entries.
        viewerTypes = viewerSettingsProviders.keys intersect openProcessors.keys,
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
