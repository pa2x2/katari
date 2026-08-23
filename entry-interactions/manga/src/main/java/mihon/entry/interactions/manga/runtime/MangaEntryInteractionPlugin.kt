package mihon.entry.interactions.manga.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryBulkDownloadCandidateCapability
import mihon.entry.interactions.download.EntryDownloadArchivePackagingCapability
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.download.EntryDownloadParallelItemTransfersCapability
import mihon.entry.interactions.download.EntryDownloadParallelSourceTransfersCapability
import mihon.entry.interactions.download.EntryDownloadTallImageSplittingCapability
import mihon.entry.interactions.library.EntryLibraryProgressCapability
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
import mihon.entry.interactions.manga.migration.MangaMigrationProvider
import mihon.entry.interactions.manga.navigation.MangaContinueProcessor
import mihon.entry.interactions.manga.navigation.MangaOpenProcessor
import mihon.entry.interactions.manga.presentation.MangaEntryTypePresentationProvider
import mihon.entry.interactions.manga.reader.MangaChildWebViewHostAdapter
import mihon.entry.interactions.manga.state.MangaConsumptionProcessor
import mihon.entry.interactions.manga.state.MangaProgressProcessor
import mihon.entry.interactions.manga.statistics.MangaEntryStatisticsProvider
import mihon.entry.interactions.media.EntryMediaCacheCapability
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsCapability
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueCapability
import mihon.entry.interactions.navigation.EntryOpenCapability
import mihon.entry.interactions.runtime.EntryChildGroupFilterCapability
import mihon.entry.interactions.runtime.EntryChildListCapability
import mihon.entry.interactions.runtime.EntryChildProgressCapability
import mihon.entry.interactions.runtime.EntryImmersiveCapability
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryMissingChildGapCapability
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterCapability
import mihon.entry.interactions.runtime.EntryPreviewCapability
import mihon.entry.interactions.runtime.EntryPreviewConfigurationCapability
import mihon.entry.interactions.runtime.EntryStatisticsCapability
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.source.EntryChildWebViewHostContribution
import mihon.entry.interactions.state.EntryBookmarkCapability
import mihon.entry.interactions.state.EntryConsumptionCapability
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryProgressCapability
import mihon.feature.graph.ContributionOwner
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun mangaEntryInteractionPlugin(
    dependencies: MangaEntryInteractionDependencies,
    viewerSettingsProvider: EntryViewerSettingsProvider? = null,
): EntryInteractionPlugin {
    return mangaEntryInteractionPlugin(
        MangaEntryInteractionRuntimeDependencies(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryChapterRepository = dependencies.entryChapterRepository,
            entryProgressRepository = dependencies.entryProgressRepository,
            downloadPreferences = dependencies.downloadPreferences,
            downloadManager = Injekt.get(),
            downloadCache = Injekt.get(),
            sourceManager = dependencies.sourceManager,
            entryRepository = Injekt.get(),
            mediaSession = dependencies.mediaSession,
            entryInteractionPreferences = dependencies.entryInteractionPreferences,
        ),
        viewerSettingsProvider = viewerSettingsProvider,
    )
}

internal fun mangaEntryInteractionPlugin(
    dependencies: MangaEntryInteractionRuntimeDependencies,
    viewerSettingsProvider: EntryViewerSettingsProvider? = null,
): EntryInteractionPlugin {
    val openProcessor = MangaOpenProcessor()
    val continueProcessor = MangaContinueProcessor(
        getEntryWithChapters = dependencies.getEntryWithChapters,
        entryProgressRepository = dependencies.entryProgressRepository,
        openProcessor = openProcessor,
    )
    val consumptionProcessor = MangaConsumptionProcessor(
        entryChapterRepository = dependencies.entryChapterRepository,
        entryProgressRepository = dependencies.entryProgressRepository,
    )
    val progressProcessor = MangaProgressProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
        entryChapterRepository = dependencies.entryChapterRepository,
    )
    val downloadProcessor = MangaDownloadProcessor(dependencies)
    val migrationProvider = MangaMigrationProvider()
    val childListProcessor = MangaChildListProcessor(dependencies.entryProgressRepository)
    val libraryProgressProvider = MangaLibraryProgressProvider(dependencies.entryProgressRepository)
    val childGroupFilterProcessor = MangaChildGroupFilterProcessor
    val outsideReleasePeriodFilterProvider = MangaOutsideReleasePeriodFilterProvider()
    val previewProcessor = MangaPreviewInteraction(dependencies.entryInteractionPreferences)
    val immersiveProcessor = MangaImmersiveProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
        mediaSession = dependencies.mediaSession,
    )
    return object : EntryInteractionPlugin {
        override val type = EntryType.MANGA
        override val owner = ContributionOwner("entry-interactions.manga")
        override val providerBindings = buildList {
            addAll(
                listOf(
                    EntryOpenCapability.bind(openProcessor),
                    EntryContinueCapability.bind(continueProcessor),
                    EntryConsumptionCapability.bind(consumptionProcessor),
                    EntryBookmarkCapability.bind(consumptionProcessor),
                    EntryProgressCapability.bind(progressProcessor),
                    EntryDownloadCapability.bind(downloadProcessor),
                    EntryDownloadArchivePackagingCapability.bind(downloadProcessor),
                    EntryDownloadTallImageSplittingCapability.bind(downloadProcessor),
                    EntryDownloadParallelSourceTransfersCapability.bind(downloadProcessor),
                    EntryDownloadParallelItemTransfersCapability.bind(downloadProcessor),
                    EntryBulkDownloadCandidateCapability.bind(downloadProcessor),
                    EntryMigrationCapability.bind(migrationProvider),
                    EntryChildListCapability.bind(childListProcessor),
                    EntryChildProgressCapability.bind(childListProcessor),
                    EntryMissingChildGapCapability.bind(childListProcessor),
                    EntryLibraryProgressCapability.bind(libraryProgressProvider),
                    EntryChildGroupFilterCapability.bind(childGroupFilterProcessor),
                    EntryOutsideReleasePeriodFilterCapability.bind(outsideReleasePeriodFilterProvider),
                    EntryPreviewCapability.bind(previewProcessor),
                    EntryPreviewConfigurationCapability.bind(previewProcessor),
                    EntryImmersiveCapability.bind(immersiveProcessor),
                    EntryMediaSessionCapability.bind(dependencies.mediaSession),
                    EntryTypePresentationCapability.bind(MangaEntryTypePresentationProvider),
                    EntryStatisticsCapability.bind(MangaEntryStatisticsProvider),
                    EntryMediaCacheCapability.bind(MangaMediaCacheProvider { Injekt.get() }),
                ),
            )
            viewerSettingsProvider?.let { add(EntryViewerSettingsCapability.bind(it)) }
        }
        override val specializedAdapters = listOf(
            EntryChildWebViewHostContribution.bind(MangaChildWebViewHostAdapter),
        )
    }
}

data class MangaEntryInteractionDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val entryProgressRepository: EntryProgressRepository,
    val downloadPreferences: DownloadPreferences,
    val sourceManager: SourceManager,
    val mediaSession: EntryMediaSessionProcessor,
    val entryInteractionPreferences: EntryInteractionPreferences,
)

internal data class MangaEntryInteractionRuntimeDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val entryProgressRepository: EntryProgressRepository,
    val downloadPreferences: DownloadPreferences,
    val downloadManager: DownloadManager,
    val downloadCache: DownloadCache,
    val sourceManager: SourceManager,
    val entryRepository: EntryRepository,
    val mediaSession: EntryMediaSessionProcessor,
    val entryInteractionPreferences: EntryInteractionPreferences,
)
