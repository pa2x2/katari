package mihon.entry.interactions

import mihon.entry.interactions.anime.animeEntryLibraryProgressCalculator
import mihon.entry.interactions.manga.mangaEntryLibraryProgressCalculator
import tachiyomi.domain.entry.repository.PlaybackStateRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator

fun entryLibraryProgressCalculators(
    playbackStateRepository: PlaybackStateRepository,
): List<EntryLibraryProgressCalculator> {
    return listOf(
        mangaEntryLibraryProgressCalculator(),
        animeEntryLibraryProgressCalculator(playbackStateRepository),
    )
}
