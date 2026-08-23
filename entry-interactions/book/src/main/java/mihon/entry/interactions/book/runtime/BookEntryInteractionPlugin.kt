package mihon.entry.interactions.book.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.child.BookChildListProcessor
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadProcessor
import mihon.entry.interactions.book.download.BookDownloadProcessorDependencies
import mihon.entry.interactions.book.library.BookLibraryProgressProvider
import mihon.entry.interactions.book.library.BookOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.book.media.BookMediaCacheProvider
import mihon.entry.interactions.book.migration.BookMigrationProvider
import mihon.entry.interactions.book.navigation.BookContinueProcessor
import mihon.entry.interactions.book.navigation.BookOpenProcessor
import mihon.entry.interactions.book.presentation.BookEntryTypePresentationProvider
import mihon.entry.interactions.book.reader.BookChildWebViewHostAdapter
import mihon.entry.interactions.book.state.BookConsumptionProcessor
import mihon.entry.interactions.book.state.BookProgressProcessor
import mihon.entry.interactions.book.statistics.BookEntryStatisticsProvider
import mihon.entry.interactions.download.EntryBulkDownloadCandidateCapability
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.library.EntryLibraryProgressCapability
import mihon.entry.interactions.media.EntryMediaCacheCapability
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsCapability
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueCapability
import mihon.entry.interactions.navigation.EntryOpenCapability
import mihon.entry.interactions.runtime.EntryChildListCapability
import mihon.entry.interactions.runtime.EntryChildProgressCapability
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterCapability
import mihon.entry.interactions.runtime.EntryStatisticsCapability
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.source.EntryChildWebViewHostContribution
import mihon.entry.interactions.state.EntryConsumptionCapability
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryProgressCapability
import mihon.feature.graph.ContributionOwner
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun bookEntryInteractionPlugin(
    dependencies: BookEntryInteractionDependencies,
    viewerSettingsProvider: EntryViewerSettingsProvider? = null,
): EntryInteractionPlugin {
    val openProcessor = BookOpenProcessor()
    val continueProcessor = BookContinueProcessor(
        getEntryWithChapters = dependencies.getEntryWithChapters,
        entryProgressRepository = dependencies.entryProgressRepository,
        openProcessor = openProcessor,
    )
    val consumptionProcessor = BookConsumptionProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
        entryChapterRepository = dependencies.entryChapterRepository,
    )
    val progressProcessor = BookProgressProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
        entryChapterRepository = dependencies.entryChapterRepository,
    )
    val downloadProcessor = if (dependencies.downloadsEnabled) {
        BookDownloadProcessor(
            BookDownloadProcessorDependencies(
                manager = Injekt.get<BookDownloadManager>(),
                cache = Injekt.get(),
                sourceManager = Injekt.get(),
                entryRepository = Injekt.get(),
                getEntryWithChapters = dependencies.getEntryWithChapters,
            ),
        )
    } else {
        null
    }
    val childListProcessor = BookChildListProcessor(dependencies.entryProgressRepository)
    val libraryProgressProvider = BookLibraryProgressProvider(dependencies.entryProgressRepository)
    val outsideReleasePeriodFilterProvider = BookOutsideReleasePeriodFilterProvider()
    val migrationProvider = BookMigrationProvider()
    return object : EntryInteractionPlugin {
        override val type = EntryType.BOOK
        override val owner = ContributionOwner("entry-interactions.book")
        override val providerBindings = buildList<EntryInteractionProviderBinding<*>> {
            add(EntryOpenCapability.bind(openProcessor))
            add(EntryContinueCapability.bind(continueProcessor))
            add(EntryConsumptionCapability.bind(consumptionProcessor))
            add(EntryProgressCapability.bind(progressProcessor))
            add(EntryChildListCapability.bind(childListProcessor))
            add(EntryChildProgressCapability.bind(childListProcessor))
            add(EntryLibraryProgressCapability.bind(libraryProgressProvider))
            add(EntryOutsideReleasePeriodFilterCapability.bind(outsideReleasePeriodFilterProvider))
            add(EntryTypePresentationCapability.bind(BookEntryTypePresentationProvider))
            add(EntryStatisticsCapability.bind(BookEntryStatisticsProvider))
            add(EntryMediaCacheCapability.bind(BookMediaCacheProvider { Injekt.get() }))
            add(EntryMediaSessionCapability.bind(dependencies.mediaSession))
            add(EntryMigrationCapability.bind(migrationProvider))
            if (downloadProcessor != null) {
                add(EntryDownloadCapability.bind(downloadProcessor))
                add(EntryBulkDownloadCandidateCapability.bind(downloadProcessor))
            }
            viewerSettingsProvider?.let { add(EntryViewerSettingsCapability.bind(it)) }
        }
        override val specializedAdapters = listOf(
            EntryChildWebViewHostContribution.bind(BookChildWebViewHostAdapter),
        )
    }
}

data class BookEntryInteractionDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val entryProgressRepository: EntryProgressRepository,
    val downloadsEnabled: Boolean = false,
    val mediaSession: EntryMediaSessionProcessor,
)
