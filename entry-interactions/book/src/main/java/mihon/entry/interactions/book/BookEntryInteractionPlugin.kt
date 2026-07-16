package mihon.entry.interactions.book

import mihon.domain.chapter.interactor.FilterEntryChaptersForDownload
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.book.download.BookDownloadCleanup
import mihon.entry.interactions.book.download.BookDownloadManager
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun bookEntryInteractionPlugin(
    dependencies: BookEntryInteractionDependencies,
): EntryInteractionPlugin {
    return EntryInteractionPlugin { registry ->
        val openProcessor = BookOpenProcessor()
        val downloadManager = if (dependencies.downloadsEnabled) Injekt.get<BookDownloadManager>() else null
        val downloadCleanup = if (dependencies.downloadsEnabled) Injekt.get<BookDownloadCleanup>() else null
        registry.registerOpenProcessor(openProcessor)
        registry.registerCapabilityProcessor(BookCapabilityProcessor())
        registry.registerChildListProcessor(BookChildListProcessor(dependencies.entryProgressRepository))
        registry.registerContinueProcessor(
            BookContinueProcessor(
                getEntryWithChapters = dependencies.getEntryWithChapters,
                entryProgressRepository = dependencies.entryProgressRepository,
                openProcessor = openProcessor,
            ),
        )
        if (dependencies.downloadsEnabled) {
            registry.registerDownloadProcessor(
                BookDownloadProcessor(
                    BookDownloadProcessorDependencies(
                        manager = checkNotNull(downloadManager),
                        cache = Injekt.get(),
                        sourceManager = Injekt.get(),
                        entryRepository = Injekt.get(),
                        getEntryWithChapters = dependencies.getEntryWithChapters,
                        filterEntryChaptersForDownload = checkNotNull(dependencies.filterEntryChaptersForDownload) {
                            "BOOK downloads require the automatic-download filter"
                        },
                        mergedEntryRepository = Injekt.get(),
                    ),
                ),
            )
        }
        registry.registerConsumptionProcessor(
            BookConsumptionProcessor(
                entryProgressRepository = dependencies.entryProgressRepository,
                entryChapterRepository = dependencies.entryChapterRepository,
                downloadCleanup = downloadCleanup,
            ),
        )
        registry.registerUpdateEligibilityProcessor(BookUpdateEligibilityProcessor())
        registry.registerProgressProcessor(
            BookProgressProcessor(
                entryProgressRepository = dependencies.entryProgressRepository,
                entryChapterRepository = dependencies.entryChapterRepository,
            ),
        )
        registry.registerLibraryFilterProcessor(BookLibraryFilterProcessor())
    }
}

data class BookEntryInteractionDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val entryProgressRepository: EntryProgressRepository,
    val filterEntryChaptersForDownload: FilterEntryChaptersForDownload? = null,
    val downloadsEnabled: Boolean = false,
)
