package app.katari.entry.interactions.anime.runtime

import app.katari.core.metro.BasePreferenceStore
import app.katari.entry.interactions.anime.child.AnimeChildListProcessor
import app.katari.entry.interactions.anime.library.AnimeLibraryProgressProvider
import app.katari.entry.interactions.anime.media.AnimePlaybackPreferencesProcessor
import app.katari.entry.interactions.anime.migration.AnimeMigrationProvider
import app.katari.entry.interactions.anime.navigation.AnimeContinueProcessor
import app.katari.entry.interactions.anime.presentation.AnimeEntryTypePresentationProvider
import app.katari.entry.interactions.anime.state.AnimeConsumptionProcessor
import app.katari.entry.interactions.anime.state.AnimeProgressProcessor
import app.katari.entry.interactions.anime.statistics.AnimeEntryStatisticsProvider
import app.katari.entry.interactions.child.ChapterScanlatorFilterProcessor
import app.katari.entry.interactions.library.EntryLibraryProgressProvider
import app.katari.entry.interactions.media.DefaultEntryViewerSettingsProvider
import app.katari.entry.interactions.media.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import app.katari.entry.interactions.media.EntryViewerSettingsProvider
import app.katari.entry.interactions.navigation.EntryContinueProcessor
import app.katari.entry.interactions.runtime.EntryChildListProcessor
import app.katari.entry.interactions.runtime.EntryChildProgressProcessor
import app.katari.entry.interactions.runtime.EntryScanlatorFilterProcessor
import app.katari.entry.interactions.runtime.EntryStatisticsProvider
import app.katari.entry.interactions.runtime.EntryTypeKey
import app.katari.entry.interactions.runtime.EntryTypePresentationProvider
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.interactions.state.EntryConsumptionProcessor
import app.katari.entry.interactions.state.EntryMigrationProvider
import app.katari.entry.interactions.state.EntryPlaybackPreferencesProcessor
import app.katari.entry.interactions.state.EntryProgressProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

/**
 * What anime contributes on every platform: watch state, the episode list, the episode continuing opens, library
 * progress, labels, statistics, migration, per-entry playback preferences and the player's settings. The player's
 * shared parts are in [AnimePlayerBindings] and downloads in [AnimeDownloadBindings]; each platform adds how episodes
 * open and the engine they play in, and Android its previews and immersive view.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeEntryTypeBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun continueTarget(): EntryContinueProcessor = AnimeContinueProcessor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun consumption(entryProgressRepository: EntryProgressRepository): EntryConsumptionProcessor =
        AnimeConsumptionProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun progress(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryProgressProcessor = AnimeProgressProcessor(entryProgressRepository, entryChapterRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun playbackPreferences(
        playbackPreferencesRepository: PlaybackPreferencesRepository,
    ): EntryPlaybackPreferencesProcessor = AnimePlaybackPreferencesProcessor(playbackPreferencesRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun migration(): EntryMigrationProvider = AnimeMigrationProvider()

    @Provides
    @SingleIn(AppScope::class)
    internal fun childListProcessor(entryProgressRepository: EntryProgressRepository): AnimeChildListProcessor =
        AnimeChildListProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun childList(processor: AnimeChildListProcessor): EntryChildListProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun scanlatorFilter(): EntryScanlatorFilterProcessor = ChapterScanlatorFilterProcessor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun childProgress(processor: AnimeChildListProcessor): EntryChildProgressProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun libraryProgress(entryProgressRepository: EntryProgressRepository): EntryLibraryProgressProvider =
        AnimeLibraryProgressProvider(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun typePresentation(): EntryTypePresentationProvider = AnimeEntryTypePresentationProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun statistics(): EntryStatisticsProvider = AnimeEntryStatisticsProvider

    @Provides
    @SingleIn(AppScope::class)
    fun viewerSettingsOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<AnimePlayerPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.anime.viewer-settings"),
        groups = setOf(
            ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
        ),
        factory = ::AnimePlayerPreferences,
    )

    @Provides
    @IntoSet
    fun viewerSettingsOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<AnimePlayerPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    fun viewerSettingsProvider(owner: InstalledProfilePreferenceOwner<AnimePlayerPreferences>): AnimePlayerPreferences =
        owner.create()

    @Provides
    @SingleIn(AppScope::class)
    fun devicePlayerPreferences(@BasePreferenceStore store: PreferenceStore): AnimePlayerDevicePreferences =
        AnimePlayerDevicePreferences(store)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun viewerSettings(preferences: AnimePlayerPreferences): EntryViewerSettingsProvider =
        DefaultEntryViewerSettingsProvider(surfaces = listOf(preferences))
}
