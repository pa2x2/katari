package mihon.entry.interactions

import mihon.entry.interactions.anime.animeEntryLibraryProgressCalculator
import mihon.entry.interactions.book.bookEntryLibraryProgressCalculator
import mihon.entry.interactions.manga.mangaEntryLibraryProgressCalculator
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator

fun entryLibraryProgressCalculators(
    entryProgressRepository: EntryProgressRepository,
): List<EntryLibraryProgressCalculator> {
    return listOf(
        mangaEntryLibraryProgressCalculator(entryProgressRepository),
        animeEntryLibraryProgressCalculator(entryProgressRepository),
        bookEntryLibraryProgressCalculator(entryProgressRepository),
    )
}
