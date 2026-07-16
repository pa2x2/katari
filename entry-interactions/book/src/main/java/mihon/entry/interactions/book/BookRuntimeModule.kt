package mihon.entry.interactions.book

import android.app.Application
import mihon.entry.interactions.EntryInteractionRuntimeContribution
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadProvider
import mihon.entry.interactions.book.epub.ReadiumEpubProcessor
import mihon.entry.interactions.book.prose.HtmlProseChapterProcessor
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

/** Installs generic BOOK host services. Built-in format processors are registered here when ready. */
fun InjektRegistrar.addBookEntryInteractionRuntime(
    app: Application,
    profilePreferenceStore: PreferenceStore,
): EntryInteractionRuntimeContribution {
    val materializationCache = BookMaterializationCache(app)
    val readiumSettingsProvider = ReadiumEpubSettingsProvider(profilePreferenceStore)
    val proseSettingsProvider = HtmlProseSettingsProvider(profilePreferenceStore)
    addSingletonFactory<BookMaterializationStore> { materializationCache }
    addSingletonFactory { BookDownloadProvider(get<StorageManager>()) }
    addSingletonFactory { BookDownloadCache(get()) }
    addSingletonFactory { BookReaderSessionRegistry() }
    addSingletonFactory { BookChapterNavigationResolver(get()) }
    addSingletonFactory { readiumSettingsProvider }
    addSingletonFactory { proseSettingsProvider }
    addSingletonFactory {
        BookProcessorRegistry(
            processors = listOf(
                ReadiumEpubProcessor(),
                HtmlProseChapterProcessor(),
            ),
        )
    }
    addSingletonFactory { BookProcessorPreferences(profilePreferenceStore) }
    addSingletonFactory {
        BookProcessorSelectionCoordinator(
            registry = get(),
            preferences = get(),
        )
    }
    addSingletonFactory {
        BookReaderHostResolver(
            sessionFactory = get(),
            selectionCoordinator = get(),
        )
    }
    addSingletonFactory {
        BookReaderSessionFactory(
            entryRepository = get(),
            entryChapterRepository = get(),
            entryProgressRepository = get(),
            historyRepository = get(),
            sourceManager = get(),
            processorRegistry = get(),
            networkHelper = get(),
            incognitoState = get<EntryReaderIncognitoState>(),
            materializationStore = get(),
            downloadCache = get(),
        )
    }
    return EntryInteractionRuntimeContribution(
        mediaCacheBuckets = listOf(materializationCache),
        viewerSettingsProviders = listOf(readiumSettingsProvider, proseSettingsProvider),
    )
}
