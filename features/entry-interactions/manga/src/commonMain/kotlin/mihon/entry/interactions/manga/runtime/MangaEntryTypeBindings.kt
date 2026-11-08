package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.child.ChapterScanlatorFilterProcessor
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.manga.child.MangaChildListProcessor
import mihon.entry.interactions.manga.library.MangaLibraryProgressProvider
import mihon.entry.interactions.manga.library.MangaOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.manga.migration.MangaMigrationProvider
import mihon.entry.interactions.manga.navigation.MangaContinueProcessor
import mihon.entry.interactions.manga.presentation.MangaEntryTypePresentationProvider
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.manga.state.MangaConsumptionProcessor
import mihon.entry.interactions.manga.state.MangaProgressProcessor
import mihon.entry.interactions.manga.state.MangaTranslationLanguagesProvider
import mihon.entry.interactions.manga.statistics.MangaEntryStatisticsProvider
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryMissingChildGapProcessor
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.runtime.ReaderSettingsSurfaceCapabilities
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * What manga contributes on every platform: reading state, the chapter list, the chapter continuing opens, library
 * progress, labels, statistics, migration and the reader's settings. The reader, downloads and page store, and what
 * needs them, such as the media session events only the reader sends, are contributed by the Android part,
 * `MangaEntryInteractionProviderBindings`; desktop stands in for the reader with `MangaViewerStandInBindings`.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaEntryTypeBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun continueTarget(): EntryContinueProcessor = MangaContinueProcessor

    @Provides
    @SingleIn(AppScope::class)
    internal fun consumptionProcessor(
        entryChapterRepository: EntryChapterRepository,
        entryProgressRepository: EntryProgressRepository,
    ): MangaConsumptionProcessor = MangaConsumptionProcessor(entryChapterRepository, entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun consumption(processor: MangaConsumptionProcessor): EntryConsumptionProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun bookmark(processor: MangaConsumptionProcessor): EntryBookmarkProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    @SingleIn(AppScope::class)
    internal fun progress(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryProgressProcessor = MangaProgressProcessor(entryProgressRepository, entryChapterRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun migration(): EntryMigrationProvider = MangaMigrationProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun translationLanguages(): EntryTranslationLanguagesProvider = MangaTranslationLanguagesProvider

    @Provides
    @SingleIn(AppScope::class)
    internal fun childListProcessor(entryProgressRepository: EntryProgressRepository): MangaChildListProcessor =
        MangaChildListProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun childList(processor: MangaChildListProcessor): EntryChildListProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun childProgress(processor: MangaChildListProcessor): EntryChildProgressProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun missingChildGap(processor: MangaChildListProcessor): EntryMissingChildGapProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    @SingleIn(AppScope::class)
    internal fun libraryProgress(entryProgressRepository: EntryProgressRepository): EntryLibraryProgressProvider =
        MangaLibraryProgressProvider(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun scanlatorFilter(): EntryScanlatorFilterProcessor = ChapterScanlatorFilterProcessor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun outsideReleasePeriodFilter(): EntryOutsideReleasePeriodFilterProvider =
        MangaOutsideReleasePeriodFilterProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun typePresentation(): EntryTypePresentationProvider = MangaEntryTypePresentationProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun statistics(): EntryStatisticsProvider = MangaEntryStatisticsProvider

    @Provides
    @SingleIn(AppScope::class)
    internal fun viewerSettingsOwner(
        owners: ProfilePreferenceOwnerInstaller,
        chapterPreparationPreferences: ReaderChapterPreparationPreferences,
    ): InstalledProfilePreferenceOwner<MangaReaderSettingsProvider> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.manga.viewer-settings"),
        groups = setOf(
            ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
        ),
        factory = { store -> MangaReaderSettingsProvider(store, chapterPreparationPreferences) },
    )

    @Provides
    @IntoSet
    internal fun viewerSettingsOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<MangaReaderSettingsProvider>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun viewerSettingsProvider(
        owner: InstalledProfilePreferenceOwner<MangaReaderSettingsProvider>,
    ): MangaReaderSettingsProvider = owner.create()

    @Provides
    internal fun readerSettings(provider: MangaReaderSettingsProvider): MangaReaderSettings = provider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    @SingleIn(AppScope::class)
    internal fun viewerSettings(provider: MangaReaderSettingsProvider): EntryViewerSettingsProvider =
        DefaultEntryViewerSettingsProvider(
            surfaces = listOf(provider),
            legacyViewerFlagsNormalization = { flags -> flags and LEGACY_MANGA_VIEWER_MASK.inv() },
        )

    @Provides
    @IntoSet
    fun readerSurfaceCapabilities(): ReaderSettingsSurfaceCapabilities = ReaderSettingsSurfaceCapabilities(
        surfaceId = MangaReaderSettings.PROVIDER_ID,
        capabilities = MangaReaderSettings.READER_CAPABILITIES,
    )
}

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL
