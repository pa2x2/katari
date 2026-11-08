package app.katari.entry.interactions.manga.navigation

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.manga.runtime.requireManga
import app.katari.entry.interactions.navigation.EntryOpenOptions
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.reader.window.MangaReaderDestination
import app.katari.entry.interactions.reader.window.MangaReaderRequest
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Opens chapters in the reader screen of the desktop window they are opened from. */
internal class MangaOpenProcessor(private val destination: MangaReaderDestination) : EntryOpenProcessor {
    override fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireManga()
        destination.open(
            context,
            MangaReaderRequest(entryId = entry.id, chapterId = chapter.id, pageIndex = options.pageIndex),
        )
    }
}
