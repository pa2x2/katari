package mihon.entry.interactions.anime.navigation

import mihon.core.platform.PlatformContext
import mihon.entry.interactions.anime.runtime.requireAnime
import mihon.entry.interactions.navigation.EntryOpenOptions
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.player.AnimePlayerDestination
import mihon.entry.interactions.player.AnimePlayerRequest
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Opens episodes in the player screen of the desktop window they are opened from. */
internal class AnimeOpenProcessor(private val destination: AnimePlayerDestination) : EntryOpenProcessor {
    override fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireAnime()
        destination.open(
            context,
            AnimePlayerRequest(
                entryId = entry.id,
                episodeId = chapter.id,
                ownerEntryId = options.ownerEntryId ?: chapter.entryId,
                bypassMerge = options.bypassMerge,
            ),
        )
    }
}
