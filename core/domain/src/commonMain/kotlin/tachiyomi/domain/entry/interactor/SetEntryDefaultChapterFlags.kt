package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryPreferences

@Inject
class SetEntryDefaultChapterFlags(
    private val libraryPreferences: LibraryPreferences,
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryIds: Collection<Long>) {
        val flags = libraryPreferences.defaultChapterFlags()
        entryIds.forEach { entryRepository.setChapterFlags(it, flags) }
    }

    /** Applies the defaults to [entryIds] and to every entry in the active profile's library. */
    suspend fun awaitWithLibrary(entryIds: Collection<Long>) {
        await(entryIds.toSet() + entryRepository.getFavorites().map { it.id })
    }
}
