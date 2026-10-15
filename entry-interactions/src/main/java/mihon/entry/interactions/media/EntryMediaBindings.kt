package mihon.entry.interactions.media

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.BasePreferenceStore
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.media.backup.EntryPlaybackPreferencesBackupParticipant
import mihon.entry.interactions.media.backup.EntryViewerSettingsBackupParticipant
import mihon.entry.interactions.media.migration.EntryPlaybackPreferencesMigrationParticipant
import mihon.entry.interactions.media.migration.EntryViewerSettingsMigrationParticipant
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.EntryPreviewConfigurationProvider
import mihon.entry.interactions.runtime.EntryPreviewProcessor
import mihon.entry.interactions.settings.DefaultViewerSettingBinder
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.entry.repository.EntryRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMediaBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun playbackPreferencesFeature(
        playbackPreferencesProcessors: Map<EntryType, EntryPlaybackPreferencesProcessor>,
        migrationProviders: Map<EntryType, EntryMigrationProvider>,
        interactions: EntryInteractions,
    ): EntryPlaybackPreferencesFeature = DefaultEntryPlaybackPreferencesFeature(
        applicableTypes = playbackPreferencesProcessors.keys,
        migrationTypes = playbackPreferencesProcessors.keys intersect migrationProviders.keys,
        interaction = interactions.playbackPreferences,
    )

    @Provides
    @IntoSet
    internal fun playbackPreferencesMigrationParticipant(
        feature: () -> EntryPlaybackPreferencesFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryPlaybackPreferencesMigrationParticipant(feature)

    @Provides
    @SingleIn(AppScope::class)
    internal fun previewFeature(
        previewProcessors: Map<EntryType, EntryPreviewProcessor>,
        previewConfigurationProviders: Map<EntryType, EntryPreviewConfigurationProvider>,
        childListProcessors: Map<EntryType, EntryChildListProcessor>,
        openProcessors: Map<EntryType, EntryOpenProcessor>,
        interactions: EntryInteractions,
        childList: EntryChildListFeature,
    ): EntryPreviewFeature = DefaultEntryPreviewFeature(
        types = EntryPreviewTypes(
            preview = previewProcessors.keys,
            configured = previewProcessors.keys intersect previewConfigurationProviders.keys,
            childList = previewProcessors.keys intersect childListProcessors.keys,
            open = previewProcessors.keys intersect openProcessors.keys,
        ),
        interaction = interactions.preview,
        childList = childList,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun immersiveFeature(
        immersiveProcessors: Map<EntryType, EntryImmersiveProcessor>,
        childListProcessors: Map<EntryType, EntryChildListProcessor>,
        openProcessors: Map<EntryType, EntryOpenProcessor>,
        interactions: EntryInteractions,
        childList: EntryChildListFeature,
        sourceRefresh: EntrySourceRefreshFeature,
    ): EntryImmersiveFeature = DefaultEntryImmersiveFeature(
        types = EntryImmersiveTypes(
            immersive = immersiveProcessors.keys,
            childList = immersiveProcessors.keys intersect childListProcessors.keys,
            open = immersiveProcessors.keys intersect openProcessors.keys,
        ),
        interaction = interactions.immersive,
        childList = childList,
        sourceRefresh = sourceRefresh,
    )

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

    @Provides
    @IntoSet
    internal fun viewerSettingsMigrationParticipant(
        feature: () -> EntryViewerSettingsFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryViewerSettingsMigrationParticipant(feature)

    @Provides
    @IntoSet
    fun viewerSettingsWarmup(feature: () -> EntryViewerSettingsFeature): StartupWarmup =
        StartupWarmup { feature() }

    @Provides
    @SingleIn(AppScope::class)
    fun mediaCacheFeature(
        mediaCacheProviders: Map<EntryType, EntryMediaCacheProvider>,
        interactions: EntryInteractions,
        @BasePreferenceStore preferenceStore: PreferenceStore,
    ): EntryMediaCacheFeature = DefaultEntryMediaCacheFeature(
        applicableTypes = mediaCacheProviders.keys,
        interaction = interactions.mediaCache,
        preferenceStore = preferenceStore,
    )

    @Provides
    @IntoSet
    internal fun playbackPreferencesBackupState(
        feature: () -> EntryPlaybackPreferencesFeature,
    ): EntryBackupStateParticipant = EntryPlaybackPreferencesBackupParticipant(feature)

    @Provides
    @IntoSet
    internal fun viewerSettingsBackupState(
        feature: () -> EntryViewerSettingsFeature,
    ): EntryBackupStateParticipant = EntryViewerSettingsBackupParticipant(feature)
}
