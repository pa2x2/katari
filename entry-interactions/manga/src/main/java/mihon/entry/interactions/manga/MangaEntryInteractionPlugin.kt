package mihon.entry.interactions.manga

import mihon.domain.chapter.interactor.FilterEntryChaptersForDownload
import mihon.entry.interactions.EntryChildGroupFilterDataSource
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.EntryReaderTracking
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun mangaEntryInteractionPlugin(
    dependencies: MangaEntryInteractionDependencies,
): EntryInteractionPlugin {
    return mangaEntryInteractionPlugin(
        MangaEntryInteractionRuntimeDependencies(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryChapterRepository = dependencies.entryChapterRepository,
            filterEntryChaptersForDownload = dependencies.filterEntryChaptersForDownload,
            childGroupFilterDataSource = dependencies.childGroupFilterDataSource,
            downloadPreferences = dependencies.downloadPreferences,
            downloadManager = Injekt.get(),
            downloadCache = Injekt.get(),
            sourceManager = dependencies.sourceManager,
            entryInteractionPreferences = dependencies.entryInteractionPreferences,
        ),
    )
}

internal fun mangaEntryInteractionPlugin(
    dependencies: MangaEntryInteractionRuntimeDependencies,
): EntryInteractionPlugin {
    return EntryInteractionPlugin { registry ->
        val openProcessor = MangaOpenProcessor()
        registry.registerOpenProcessor(openProcessor)
        registry.registerCapabilityProcessor(MangaCapabilityProcessor())
        registry.registerChildListProcessor(MangaChildListProcessor())
        registry.registerContinueProcessor(
            MangaContinueProcessor(
                getEntryWithChapters = dependencies.getEntryWithChapters,
                openProcessor = openProcessor,
            ),
        )
        registry.registerDownloadProcessor(
            MangaDownloadProcessor(
                dependencies = dependencies,
            ),
        )
        registry.registerConsumptionProcessor(
            MangaConsumptionProcessor(
                entryChapterRepository = dependencies.entryChapterRepository,
                downloadPreferences = dependencies.downloadPreferences,
                downloadManager = dependencies.downloadManager,
                sourceManager = dependencies.sourceManager,
            ),
        )
        registry.registerUpdateEligibilityProcessor(MangaUpdateEligibilityProcessor())
        registry.registerChildGroupFilterProcessor(
            MangaChildGroupFilterProcessor(
                dataSource = dependencies.childGroupFilterDataSource,
            ),
        )
        registry.registerLibraryFilterProcessor(MangaLibraryFilterProcessor())
        val previewInteraction = MangaPreviewInteraction(
            entryInteractionPreferences = dependencies.entryInteractionPreferences,
        )
        registry.registerPreviewProcessor(previewInteraction)
        registry.registerImmersiveProcessor(
            MangaImmersiveProcessor(
                entryChapterRepository = dependencies.entryChapterRepository,
                historyRepository = runCatching { Injekt.get<HistoryRepository>() }.getOrNull(),
                readerIncognitoState = runCatching { Injekt.get<EntryReaderIncognitoState>() }.getOrNull(),
                readerTracking = runCatching { Injekt.get<EntryReaderTracking>() }.getOrNull(),
            ),
        )
    }
}

data class MangaEntryInteractionDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val filterEntryChaptersForDownload: FilterEntryChaptersForDownload,
    val childGroupFilterDataSource: EntryChildGroupFilterDataSource,
    val downloadPreferences: DownloadPreferences,
    val sourceManager: SourceManager,
    val entryInteractionPreferences: EntryInteractionPreferences,
)

internal data class MangaEntryInteractionRuntimeDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val filterEntryChaptersForDownload: FilterEntryChaptersForDownload,
    val childGroupFilterDataSource: EntryChildGroupFilterDataSource,
    val downloadPreferences: DownloadPreferences,
    val downloadManager: DownloadManager,
    val downloadCache: DownloadCache,
    val sourceManager: SourceManager,
    val entryInteractionPreferences: EntryInteractionPreferences,
)
