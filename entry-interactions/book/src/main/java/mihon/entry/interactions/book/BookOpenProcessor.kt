package mihon.entry.interactions.book

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryOpenProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class BookOpenProcessor(
    private val createIntent: (Context, Entry, EntryChapter) -> Intent = BookReaderHostActivity::newIntent,
) : EntryOpenProcessor {
    override val type: EntryType = EntryType.BOOK

    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        context.startActivity(intent(context, entry, chapter, options))
    }

    override fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            chapter.id.hashCode(),
            intent(context, entry, chapter, options),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
