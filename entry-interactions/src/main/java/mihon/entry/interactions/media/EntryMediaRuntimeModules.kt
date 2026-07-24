package mihon.entry.interactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mihon.entry.interactions.settings.DefaultViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryPlaybackPreferencesFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.playback-preferences-transfer",
    contributor = EntryPlaybackPreferencesFeatureContributor,
    additionalContributors = listOf(
        EntryPlaybackPreferencesBackupContributor,
        EntryPlaybackPreferencesMigrationContributor,
    ),
) {
    addSingletonFactory<EntryPlaybackPreferencesFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryPlaybackPreferencesFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.playbackPreferences,
        )
    }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryPlaybackPreferencesMigrationBinding { get<EntryPlaybackPreferencesFeature>() },
        ),
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_PLAYBACK_PREFERENCES_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    when (val result = get<EntryPlaybackPreferencesFeature>().snapshot(event.entry)) {
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
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_PLAYBACK_PREFERENCES_BACKUP_RESTORE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = event.states.decodeEntryBackupState(
                        ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID,
                        ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
                        EntryPlaybackPreferencesSnapshot.serializer(),
                    ) ?: return@FeatureExecutionHandler
                    get<EntryPlaybackPreferencesFeature>().restore(event.entry, state)
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryPlaybackPreferencesFeature>() }),
    )
}

internal val EntryPreviewFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.preview",
    contributor = EntryPreviewFeatureContributor,
) {
    addSingletonFactory<EntryPreviewFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryPreviewFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.preview,
            childList = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryPreviewFeature>() }),
    )
}

internal val EntryImmersiveFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.immersive",
    contributor = EntryImmersiveFeatureContributor,
) {
    addSingletonFactory<EntryImmersiveFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryImmersiveFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.immersive,
            childList = get(),
            sourceRefresh = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryImmersiveFeature>() }),
    )
}

internal val EntryViewerSettingsFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.viewer-settings",
    contributor = EntryViewerSettingsFeatureContributor,
    additionalContributors = listOf(EntryViewerSettingsBackupContributor, EntryViewerSettingsMigrationContributor),
) { context ->
    addSingletonFactory<ViewerSettingBinder> {
        DefaultViewerSettingBinder(
            overrideRepository = get<ViewerSettingOverrideRepository>(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    addSingletonFactory<EntryViewerSettingsFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryViewerSettingsFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.viewerSettings,
            projectionResolver = context.dependencies.viewerSettingsScreenProjectionResolver,
            overrideRepository = get<ViewerSettingOverrideRepository>(),
            legacyMangaViewerFlagsReset = EntryLegacyMangaViewerFlagsReset {
                get<EntryRepository>().resetViewerFlags()
            },
            migrationStore = EntryViewerFlagsMigrationStore { entryId, profileId, viewerFlags ->
                val repository = get<EntryRepository>()
                repository.getEntryById(entryId, profileId)
                    ?.let { current -> repository.update(current.copy(viewerFlags = viewerFlags), profileId) }
                    ?: false
            },
        )
    }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryViewerSettingsMigrationBinding { get<EntryViewerSettingsFeature>() },
        ),
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_VIEWER_SETTINGS_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val result = get<EntryViewerSettingsFeature>().snapshot(event.entry)
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
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_VIEWER_SETTINGS_BACKUP_RESTORE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = event.states.decodeEntryBackupState(
                        ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID,
                        ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION,
                        EntryViewerSettingsBackupState.serializer(),
                    ) ?: return@FeatureExecutionHandler
                    get<EntryViewerSettingsFeature>().restore(
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
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryViewerSettingsFeature>() }),
        warmups = listOf { get<EntryViewerSettingsFeature>() },
    )
}

internal val EntryMediaCacheFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.media-cache",
    contributor = EntryMediaCacheFeatureContributor,
) { context ->
    addSingletonFactory<EntryMediaCacheFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryMediaCacheFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.mediaCache,
            preferenceStore = context.dependencies.basePreferenceStore,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryMediaCacheFeature>() }),
    )
}
