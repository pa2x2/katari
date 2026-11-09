package app.katari.entry.interactions.anime.navigation

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.anime.runtime.requireAnime
import app.katari.entry.interactions.navigation.EntryOpenOptions
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.player.AnimePlayerDestination
import app.katari.entry.interactions.player.AnimePlayerRequest
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
                startPositionMs = options.startPositionMs,
            ),
        )
    }
}
