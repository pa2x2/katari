package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.download.EntryDownloadSetting
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.manga.child.MangaChildGroupFilterProcessor
import mihon.entry.interactions.manga.child.MangaChildListProcessor
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.MangaDownloadProcessor
import mihon.entry.interactions.manga.library.MangaLibraryProgressProvider
import mihon.entry.interactions.manga.library.MangaOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.manga.media.MangaImmersiveProcessor
import mihon.entry.interactions.manga.media.MangaMediaCacheProvider
import mihon.entry.interactions.manga.media.MangaPreviewInteraction
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.migration.MangaMigrationProvider
import mihon.entry.interactions.manga.navigation.MangaContinueProcessor
import mihon.entry.interactions.manga.navigation.MangaOpenProcessor
import mihon.entry.interactions.manga.presentation.MangaEntryTypePresentationProvider
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.manga.state.MangaConsumptionProcessor
import mihon.entry.interactions.manga.state.MangaProgressProcessor
import mihon.entry.interactions.manga.state.MangaTranslationLanguagesProvider
import mihon.entry.interactions.manga.statistics.MangaEntryStatisticsProvider
import mihon.entry.interactions.manga.translation.background.MangaEntryTranslateProcessor
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.runtime.EntryChildGroupFilterProcessor
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryMissingChildGapProcessor
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.runtime.EntryPageImageCache
import mihon.entry.interactions.runtime.EntryPreviewConfigurationProvider
import mihon.entry.interactions.runtime.EntryPreviewProcessor
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import mihon.entry.interactions.translate.EntryTranslateProcessor
import mihon.entry.viewer.settings.ViewerSettingBinder
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object MangaEntryInteractionProviderBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun openProcessor(): MangaOpenProcessor = MangaOpenProcessor()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun open(processor: MangaOpenProcessor): EntryOpenProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    @SingleIn(AppScope::class)
    internal fun continueEntry(openProcessor: MangaOpenProcessor): EntryContinueProcessor =
        MangaContinueProcessor(openProcessor)

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
    @SingleIn(AppScope::class)
    internal fun downloadProcessor(
        downloadManager: DownloadManager,
        downloadCache: DownloadCache,
        sourceManager: SourceManager,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): MangaDownloadProcessor = MangaDownloadProcessor(
        downloadManager = downloadManager,
        downloadCache = downloadCache,
        sourceManager = sourceManager,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun download(processor: MangaDownloadProcessor): EntryDownloadProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun bulkDownloadCandidates(processor: MangaDownloadProcessor): EntryBulkDownloadCandidateProcessor =
        processor

    @Provides
    @ElementsIntoSet
    internal fun downloadSettings(): Set<EntryDownloadSetting> = EntryDownloadSetting.entries.toSet()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun migration(): EntryMigrationProvider = MangaMigrationProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun translationLanguages(): EntryTranslationLanguagesProvider = MangaTranslationLanguagesProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun translate(processor: MangaEntryTranslateProcessor): EntryTranslateProcessor = processor

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
    internal fun childGroupFilter(): EntryChildGroupFilterProcessor = MangaChildGroupFilterProcessor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun outsideReleasePeriodFilter(): EntryOutsideReleasePeriodFilterProvider =
        MangaOutsideReleasePeriodFilterProvider()

    @Provides
    @SingleIn(AppScope::class)
    internal fun previewProcessor(
        entryInteractionPreferences: EntryInteractionPreferences,
        pageSessionLoader: Lazy<ReaderPageSessionLoader>,
    ): MangaPreviewInteraction = MangaPreviewInteraction(entryInteractionPreferences, pageSessionLoader)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun preview(processor: MangaPreviewInteraction): EntryPreviewProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun previewConfiguration(processor: MangaPreviewInteraction): EntryPreviewConfigurationProvider =
        processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    @SingleIn(AppScope::class)
    internal fun immersive(
        entryProgressRepository: EntryProgressRepository,
        mediaSession: MangaMediaSessionProcessor,
        pageSessionLoader: Lazy<ReaderPageSessionLoader>,
        readerSettings: MangaReaderSettings,
        viewerSettingBinder: ViewerSettingBinder,
    ): EntryImmersiveProcessor = MangaImmersiveProcessor(
        entryProgressRepository = entryProgressRepository,
        mediaSession = mediaSession,
        loadPageSession = { context, entry, chapter -> pageSessionLoader.value.load(context, entry, chapter) },
        bindKeepScreenOn = { entryId -> viewerSettingBinder.bind(readerSettings.display.keepScreenOn, entryId) },
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun mediaSession(processor: MangaMediaSessionProcessor): EntryMediaSessionProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun typePresentation(): EntryTypePresentationProvider = MangaEntryTypePresentationProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun statistics(): EntryStatisticsProvider = MangaEntryStatisticsProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun mediaCache(pageImageCache: () -> EntryPageImageCache): EntryMediaCacheProvider =
        MangaMediaCacheProvider(pageImageCache)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    @SingleIn(AppScope::class)
    internal fun viewerSettings(provider: MangaReaderSettingsProvider): EntryViewerSettingsProvider =
        DefaultEntryViewerSettingsProvider(
            surfaces = listOf(provider),
            legacyViewerFlagsNormalization = { flags -> flags and LEGACY_MANGA_VIEWER_MASK.inv() },
        )
}

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL
