package mihon.entry.interactions.book.runtime

import android.app.Application
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadIndexStore
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadProvider
import mihon.entry.interactions.book.download.BookDownloadStore
import mihon.entry.interactions.book.download.BookDownloader
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorPreferences
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorSelectionCoordinator
import mihon.entry.interactions.book.reader.BookReaderHostResolver
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.BookReaderSessionRegistry
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationPreferences
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.runtime.EntryTypeRuntimeContribution
import mihon.entry.interactions.runtime.EntryTypeRuntimeModule
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

fun bookEntryTypeRuntimeModule(profilePreferenceOwners: ProfilePreferenceOwnerInstaller): EntryTypeRuntimeModule {
    return EntryTypeRuntimeModule(EntryType.BOOK) { app ->
        val runtime = addBookEntryInteractionRuntime(app, profilePreferenceOwners)
        val mediaSession = BookMediaSessionProcessor(get<EntryMediaSessionEventSink>())
        addSingletonFactory { mediaSession }
        val progressRepository = get<EntryProgressRepository>()
        EntryTypeRuntimeContribution(
            plugin = bookEntryInteractionPlugin(
                BookEntryInteractionDependencies(
                    getEntryWithChapters = get(),
                    entryChapterRepository = get(),
                    entryProgressRepository = progressRepository,
                    downloadsEnabled = true,
                    mediaSession = mediaSession,
                ),
            ),
            potentialReaderCapabilitiesBySettingsSurface = runtime.potentialReaderCapabilitiesBySettingsSurface,
            sharedReaderSettingsProviderFactories = listOf(
                { get<BookAutomaticTranslationSettingsProvider>() },
            ),
        )
    }
}

/** Installs generic BOOK host services and composes built-in preparers with reader implementations. */
private fun InjektRegistrar.addBookEntryInteractionRuntime(
    app: Application,
    profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
): BookRuntimeArtifacts {
    val materializationCache = BookMaterializationCache(app)
    val processorPreferencesOwner = profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.processor-selection"),
        keyPatterns = BookReaderProcessorPreferences.profileKeyPatterns,
        factory = ::BookReaderProcessorPreferences,
    )
    val automaticTranslationPreferencesOwner = profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.automatic-translation"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX),
        ),
        factory = ::BookAutomaticTranslationPreferences,
    )
    val automaticTranslationPreferences = automaticTranslationPreferencesOwner.create()
    val preparerRegistry = BookContentPreparerRegistry(preparers = emptyList())
    val readerProcessorRegistry = BookReaderProcessorRegistry(processors = emptyList())
    addSingletonFactory { materializationCache }
    addSingletonFactory<BookMaterializationStore> { get<BookMaterializationCache>() }
    addSingletonFactory { BookDownloadProvider(get<StorageManager>()) }
    addSingletonFactory {
        val storageManager = get<StorageManager>()
        BookDownloadCache(
            provider = get(),
            indexStore = BookDownloadIndexStore(app),
            storageChanges = storageManager.changes,
        )
    }
    addSingletonFactory { BookDownloadStore(app) }
    addSingletonFactory { BookReaderSessionRegistry() }
    addSingletonFactory { BookChapterNavigationResolver(get()) }
    addSingletonFactory { automaticTranslationPreferences }
    addSingletonFactory { preparerRegistry }
    addSingletonFactory { readerProcessorRegistry }
    addSingletonFactory {
        BookAutomaticTranslationSettingsProvider(
            processorRegistry = readerProcessorRegistry,
            preferences = automaticTranslationPreferences,
            translationHostActions = get<TranslationHostActions>(),
        )
    }
    addSingletonFactory {
        BookDownloader(
            application = app,
            provider = get(),
            cache = get(),
            sourceManager = get(),
            networkHelper = get(),
            materializationStore = get(),
            preparerRegistry = get(),
        )
    }
    addSingletonFactory {
        BookDownloadManager(
            context = app,
            cache = get(),
            provider = get(),
            downloader = get(),
            sourceManager = get(),
            store = get(),
        )
    }
    addSingletonFactory { processorPreferencesOwner.create() }
    addSingletonFactory {
        BookReaderProcessorSelectionCoordinator(
            registry = get(),
            preferences = get(),
        )
    }
    addSingletonFactory {
        BookReaderHostResolver(
            sessionFactory = get(),
            preparerRegistry = get(),
            selectionCoordinator = get(),
        )
    }
    addSingletonFactory {
        BookReaderSessionFactory(
            entryRepository = get(),
            entryChapterRepository = get(),
            entryProgressRepository = get(),
            sourceManager = get(),
            preparerRegistry = get(),
            readerProcessorRegistry = get(),
            networkHelper = get(),
            materializationStore = get(),
            downloadCache = get(),
            mediaSession = get<BookMediaSessionProcessor>(),
        )
    }
    return BookRuntimeArtifacts(
        potentialReaderCapabilitiesBySettingsSurface =
        readerProcessorRegistry.potentialReaderCapabilitiesBySettingsSurface(),
    )
}

private data class BookRuntimeArtifacts(
    val potentialReaderCapabilitiesBySettingsSurface: Map<String, Set<ReaderCapabilityId>>,
)
