package app.katari.entry.interactions.manga.navigation

import android.content.Context
import app.katari.entry.interactions.manga.runtime.requireManga
import app.katari.entry.interactions.navigation.EntryOpenOptions
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class MangaOpenProcessor(
    private val openChapter: (Context, Entry, EntryChapter, EntryOpenOptions) -> Unit = ::openMangaChapter,
) : EntryOpenProcessor {
    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireManga()
        openChapter(context, entry, chapter, options)
    }
}
