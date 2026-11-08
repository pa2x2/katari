package mihon.entry.interactions.manga.navigation

import mihon.core.platform.PlatformContext
import mihon.entry.interactions.manga.runtime.requireManga
import mihon.entry.interactions.navigation.EntryOpenOptions
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.reader.window.MangaReaderDestination
import mihon.entry.interactions.reader.window.MangaReaderRequest
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
