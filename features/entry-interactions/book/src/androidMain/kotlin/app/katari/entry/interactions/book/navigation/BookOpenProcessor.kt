package app.katari.entry.interactions.book.navigation

import android.content.Context
import android.content.Intent
import app.katari.entry.interactions.book.reader.BookReaderHostActivity
import app.katari.entry.interactions.book.runtime.requireBook
import app.katari.entry.interactions.navigation.EntryOpenOptions
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class BookOpenProcessor(
    private val createIntent: (Context, Entry, EntryChapter) -> Intent = BookReaderHostActivity::newIntent,
) : EntryOpenProcessor {
    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        context.startActivity(intent(context, entry, chapter, options))
    }

    private fun intent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): Intent {
        entry.requireBook()
        return createIntent(context, entry, chapter).apply {
            if (options.newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (options.clearTop) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}
