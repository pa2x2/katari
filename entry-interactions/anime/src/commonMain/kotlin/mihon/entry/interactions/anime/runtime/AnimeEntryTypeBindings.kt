package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.metro.BasePreferenceStore
import mihon.entry.interactions.anime.child.AnimeChildListProcessor
import mihon.entry.interactions.anime.library.AnimeLibraryProgressProvider
import mihon.entry.interactions.anime.media.AnimePlaybackPreferencesProcessor
import mihon.entry.interactions.anime.migration.AnimeMigrationProvider
import mihon.entry.interactions.anime.presentation.AnimeEntryTypePresentationProvider
import mihon.entry.interactions.anime.state.AnimeConsumptionProcessor
import mihon.entry.interactions.anime.state.AnimeProgressProcessor
import mihon.entry.interactions.anime.statistics.AnimeEntryStatisticsProvider
import mihon.entry.interactions.child.ChapterScanlatorFilterProcessor
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.settings.AnimePlayerDevicePreferences
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.state.EntryProgressProcessor
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

/**
 * What anime contributes on every platform: watch state, the episode list, library progress, labels, statistics,
 * migration, per-entry playback preferences and the player's settings. The player, downloads and previews, and what
 * needs them, such as the media session events only the player sends, are contributed by the Android part,
 * `AnimeEntryInteractionProviderBindings`.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeEntryTypeBindings {
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
