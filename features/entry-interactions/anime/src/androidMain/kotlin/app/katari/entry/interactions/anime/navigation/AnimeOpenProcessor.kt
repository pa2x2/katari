package app.katari.entry.interactions.anime.navigation

import android.content.Context
import app.katari.entry.interactions.anime.runtime.requireAnime
import app.katari.entry.interactions.navigation.EntryOpenOptions
import app.katari.entry.interactions.navigation.EntryOpenProcessor
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
