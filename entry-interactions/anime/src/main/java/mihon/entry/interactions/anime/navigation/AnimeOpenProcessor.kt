package mihon.entry.interactions.anime.navigation

import android.content.Context
import mihon.entry.interactions.anime.runtime.requireAnime
import mihon.entry.interactions.navigation.EntryOpenOptions
import mihon.entry.interactions.navigation.EntryOpenProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class AnimeOpenProcessor(
    private val openEpisode: (Context, Entry, EntryChapter, EntryOpenOptions) -> Unit = ::openAnimeEpisode,
) : EntryOpenProcessor {
    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireAnime()
        openEpisode(context, entry, chapter, options)
    }
}
