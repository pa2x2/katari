package mihon.entry.interactions.book

import android.app.Application
import mihon.entry.interactions.EntryMediaCacheBucket
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.book.epub.ReadiumEpubProcessor
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

/** Installs generic BOOK host services. Built-in format processors are registered here when ready. */
fun InjektRegistrar.addBookEntryInteractionRuntime(
    app: Application,
    profilePreferenceStore: PreferenceStore,
): EntryMediaCacheBucket {
    val materializationCache = BookMaterializationCache(app)
    addSingletonFactory<BookMaterializationStore> { materializationCache }
    addSingletonFactory { BookProcessorRegistry(processors = listOf(ReadiumEpubProcessor())) }
    addSingletonFactory { BookProcessorPreferences(profilePreferenceStore) }
    addSingletonFactory {
        BookProcessorSelectionCoordinator(
            registry = get(),
            preferences = get(),
        )
    }
    addSingletonFactory {
        BookReaderHostResolver(
            entryRepository = get(),
            entryChapterRepository = get(),
            sourceManager = get(),
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
    return materializationCache
}
