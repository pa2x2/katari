package mihon.entry.interactions.media

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.media.backup.EntryIntroTimesMatchesBackupParticipant
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
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import tachiyomi.domain.entry.repository.IntroTimesMatchRepository

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
    @IntoSet
    internal fun viewerSettingsMigrationParticipant(
        feature: () -> EntryViewerSettingsFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryViewerSettingsMigrationParticipant(feature)

    @Provides
    @IntoSet
    fun viewerSettingsWarmup(feature: () -> EntryViewerSettingsFeature): StartupWarmup =
        StartupWarmup { feature() }

    @Provides
    @IntoSet
    internal fun playbackPreferencesBackupState(
        feature: () -> EntryPlaybackPreferencesFeature,
    ): EntryBackupStateParticipant = EntryPlaybackPreferencesBackupParticipant(feature)

    @Provides
    @IntoSet
    internal fun introTimesMatchesBackupState(
        repository: () -> IntroTimesMatchRepository,
    ): EntryBackupStateParticipant = EntryIntroTimesMatchesBackupParticipant(repository)

    @Provides
    @IntoSet
    internal fun viewerSettingsBackupState(
        feature: () -> EntryViewerSettingsFeature,
    ): EntryBackupStateParticipant = EntryViewerSettingsBackupParticipant(feature)
}
