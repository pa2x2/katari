package mihon.entry.interactions.book.navigation

import mihon.core.platform.PlatformContext
import mihon.entry.interactions.book.runtime.requireBook
import mihon.entry.interactions.navigation.EntryOpenOptions
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.reader.window.BookReaderDestination
import mihon.entry.interactions.reader.window.BookReaderWindowRequest
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Opens chapters in the book reader screen of the desktop window they are opened from. */
internal class BookOpenProcessor(private val destination: BookReaderDestination) : EntryOpenProcessor {
    override fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireBook()
        destination.open(context, BookReaderWindowRequest(entryId = entry.id, chapterId = chapter.id))
    }
}
