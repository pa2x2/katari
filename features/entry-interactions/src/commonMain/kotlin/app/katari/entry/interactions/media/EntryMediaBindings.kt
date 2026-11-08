package app.katari.entry.interactions.media

import app.katari.core.metro.StartupWarmup
import app.katari.entry.interactions.child.EntryChildListFeature
import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.media.backup.EntryIntroTimesMatchesBackupParticipant
import app.katari.entry.interactions.media.backup.EntryPlaybackPreferencesBackupParticipant
import app.katari.entry.interactions.media.backup.EntryTheIntroDbMatchesBackupParticipant
import app.katari.entry.interactions.media.backup.EntryViewerSettingsBackupParticipant
import app.katari.entry.interactions.media.migration.EntryPlaybackPreferencesMigrationParticipant
import app.katari.entry.interactions.media.migration.EntryViewerSettingsMigrationParticipant
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.runtime.EntryChildListProcessor
import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.runtime.EntryPreviewConfigurationProvider
import app.katari.entry.interactions.runtime.EntryPreviewProcessor
import app.katari.entry.interactions.state.EntryMigrationProvider
import app.katari.entry.interactions.state.EntryPlaybackPreferencesProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.repository.IntroTimesMatchRepository
import tachiyomi.domain.entry.repository.TheIntroDbMatchRepository

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
    internal fun theIntroDbMatchesBackupState(
        repository: () -> TheIntroDbMatchRepository,
    ): EntryBackupStateParticipant = EntryTheIntroDbMatchesBackupParticipant(repository)

    @Provides
    @IntoSet
    internal fun viewerSettingsBackupState(
        feature: () -> EntryViewerSettingsFeature,
    ): EntryBackupStateParticipant = EntryViewerSettingsBackupParticipant(feature)
}
