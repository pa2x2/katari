package mihon.entry.interactions.book

import android.app.Application
import mihon.entry.interactions.EntryInteractionRuntimeContribution
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.book.epub.ReadiumEpubProcessor
import mihon.entry.interactions.book.prose.HtmlProseChapterProcessor
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import tachiyomi.core.common.preference.PreferenceStore
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
    addSingletonFactory<BookMaterializationStore> { materializationCache }
    addSingletonFactory { BookReaderSessionRegistry() }
    addSingletonFactory { BookChapterNavigationResolver(get()) }
    addSingletonFactory { readiumSettingsProvider }
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
        )
    }
    return EntryInteractionRuntimeContribution(
        mediaCacheBuckets = listOf(materializationCache),
        viewerSettingsProviders = listOf(readiumSettingsProvider),
    )
}
