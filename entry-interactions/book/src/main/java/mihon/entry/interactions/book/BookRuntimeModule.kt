package mihon.entry.interactions.book

import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

/** Installs generic BOOK host services. Built-in format processors are registered here when ready. */
fun InjektRegistrar.addBookEntryInteractionRuntime(
    profilePreferenceStore: PreferenceStore,
) {
    addSingletonFactory { BookProcessorRegistry(processors = emptyList()) }
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
}
