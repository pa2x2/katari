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
import mihon.entry.interactions.media.backup.ENTRY_PLAYBACK_PREFERENCES_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.media.backup.ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.media.backup.ENTRY_PLAYBACK_PREFERENCES_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.media.backup.ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID
import mihon.entry.interactions.media.backup.ENTRY_VIEWER_SETTINGS_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.media.backup.ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.media.backup.ENTRY_VIEWER_SETTINGS_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.media.backup.ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID
import mihon.entry.interactions.media.backup.EntryPlaybackPreferencesBackupContributor
import mihon.entry.interactions.media.backup.EntryViewerSettingBackupValue
import mihon.entry.interactions.media.backup.EntryViewerSettingsBackupContributor
import mihon.entry.interactions.media.backup.EntryViewerSettingsBackupState
import mihon.entry.interactions.media.migration.EntryPlaybackPreferencesMigrationContributor
import mihon.entry.interactions.media.migration.EntryViewerSettingsMigrationContributor
import mihon.entry.interactions.media.migration.entryPlaybackPreferencesMigrationBinding
import mihon.entry.interactions.media.migration.entryViewerSettingsMigrationBinding
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.settings.DefaultViewerSettingBinder
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
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
    fun playbackPreferencesBackupGraphContributor(): FeatureGraphContributor =
        EntryPlaybackPreferencesBackupContributor

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
    @IntoSet
    fun playbackPreferencesBackupSnapshotParticipant(
        feature: () -> EntryPlaybackPreferencesFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_PLAYBACK_PREFERENCES_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            when (val result = feature().snapshot(event.entry)) {
                is EntryPlaybackPreferencesSnapshotResult.Captured -> event.contributions.add(
                    entryBackupStateEnvelope(
                        ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID,
                        ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
                        EntryPlaybackPreferencesSnapshot.serializer(),
                        result.snapshot,
                    ),
                )

                is EntryPlaybackPreferencesSnapshotResult.Inapplicable,
                EntryPlaybackPreferencesSnapshotResult.NoPreferences,
                -> Unit
            }
        },
    )

    @Provides
    @IntoSet
    fun playbackPreferencesBackupRestoreParticipant(
        feature: () -> EntryPlaybackPreferencesFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_PLAYBACK_PREFERENCES_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID,
                ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
                EntryPlaybackPreferencesSnapshot.serializer(),
            ) ?: return@FeatureExecutionHandler
            feature().restore(event.entry, state)
        },
    )

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
    fun viewerSettingsBackupGraphContributor(): FeatureGraphContributor = EntryViewerSettingsBackupContributor

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
    fun viewerSettingsBackupSnapshotParticipant(
        feature: () -> EntryViewerSettingsFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_VIEWER_SETTINGS_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val result = feature().snapshot(event.entry)
            if (result is EntryViewerSettingsSnapshotResult.Available && result.overrides.isNotEmpty()) {
                val state = EntryViewerSettingsBackupState(
                    result.overrides.map { override ->
                        EntryViewerSettingBackupValue(
                            providerId = override.settingId.providerId,
                            settingKey = override.settingId.key,
                            encodedValue = override.encodedValue,
                            updatedAt = override.updatedAt,
                        )
                    },
                )
                event.contributions.add(
                    entryBackupStateEnvelope(
                        ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID,
                        ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION,
                        EntryViewerSettingsBackupState.serializer(),
                        state,
                    ),
                )
            }
        },
    )

    @Provides
    @IntoSet
    fun viewerSettingsBackupRestoreParticipant(
        feature: () -> EntryViewerSettingsFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_VIEWER_SETTINGS_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID,
                ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION,
                EntryViewerSettingsBackupState.serializer(),
            ) ?: return@FeatureExecutionHandler
            feature().restore(
                event.entry,
                state.overrides.map { value ->
                    ViewerSettingOverride(
                        entryId = event.entry.id,
                        settingId = ViewerSettingId(value.providerId, value.settingKey),
                        encodedValue = value.encodedValue,
                        updatedAt = value.updatedAt,
                    )
                },
            )
        },
    )

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
}
