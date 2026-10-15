package mihon.entry.interactions.media

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.BasePreferenceStore
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.media.backup.EntryPlaybackPreferencesBackupParticipant
import mihon.entry.interactions.media.backup.EntryViewerSettingsBackupParticipant
import mihon.entry.interactions.media.migration.EntryPlaybackPreferencesMigrationContributor
import mihon.entry.interactions.media.migration.EntryViewerSettingsMigrationContributor
import mihon.entry.interactions.media.migration.entryPlaybackPreferencesMigrationBinding
import mihon.entry.interactions.media.migration.entryViewerSettingsMigrationBinding
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.settings.DefaultViewerSettingBinder
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeWarmup
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.entry.repository.EntryRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMediaBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun playbackPreferencesFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
    ): EntryPlaybackPreferencesFeature = DefaultEntryPlaybackPreferencesFeature(
        evaluation = composition.evaluation,
        interaction = interactions.playbackPreferences,
    )

    @Provides
    @IntoSet
    fun playbackPreferencesGraphContributor(): FeatureGraphContributor = EntryPlaybackPreferencesFeatureContributor

    @Provides
    @IntoSet
    fun playbackPreferencesMigrationGraphContributor(): FeatureGraphContributor =
        EntryPlaybackPreferencesMigrationContributor

    @Provides
    @IntoSet
    fun playbackPreferencesMigrationParticipant(
        feature: () -> EntryPlaybackPreferencesFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryPlaybackPreferencesMigrationBinding(feature)

    @Provides
    @SingleIn(AppScope::class)
    fun previewFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        childList: EntryChildListFeature,
    ): EntryPreviewFeature = DefaultEntryPreviewFeature(
        evaluation = composition.evaluation,
        interaction = interactions.preview,
        childList = childList,
    )

    @Provides
    @IntoSet
    fun previewGraphContributor(): FeatureGraphContributor = EntryPreviewFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun immersiveFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        childList: EntryChildListFeature,
        sourceRefresh: EntrySourceRefreshFeature,
    ): EntryImmersiveFeature = DefaultEntryImmersiveFeature(
        evaluation = composition.evaluation,
        interaction = interactions.immersive,
        childList = childList,
        sourceRefresh = sourceRefresh,
    )

    @Provides
    @IntoSet
    fun immersiveGraphContributor(): FeatureGraphContributor = EntryImmersiveFeatureContributor

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
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        projectionResolver: EntryViewerSettingsScreenProjectionResolver,
        overrideRepository: ViewerSettingOverrideRepository,
        entryRepository: () -> EntryRepository,
    ): EntryViewerSettingsFeature = DefaultEntryViewerSettingsFeature(
        evaluation = composition.evaluation,
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
    fun viewerSettingsGraphContributor(): FeatureGraphContributor = EntryViewerSettingsFeatureContributor

    @Provides
    @IntoSet
    fun viewerSettingsMigrationGraphContributor(): FeatureGraphContributor = EntryViewerSettingsMigrationContributor

    @Provides
    @IntoSet
    fun viewerSettingsMigrationParticipant(
        feature: () -> EntryViewerSettingsFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryViewerSettingsMigrationBinding(feature)

    @Provides
    @IntoSet
    fun viewerSettingsWarmup(feature: () -> EntryViewerSettingsFeature): FeatureRuntimeWarmup =
        FeatureRuntimeWarmup { feature() }

    @Provides
    @SingleIn(AppScope::class)
    fun mediaCacheFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        @BasePreferenceStore preferenceStore: PreferenceStore,
    ): EntryMediaCacheFeature = DefaultEntryMediaCacheFeature(
        evaluation = composition.evaluation,
        interaction = interactions.mediaCache,
        preferenceStore = preferenceStore,
    )

    @Provides
    @IntoSet
    fun mediaCacheGraphContributor(): FeatureGraphContributor = EntryMediaCacheFeatureContributor

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
