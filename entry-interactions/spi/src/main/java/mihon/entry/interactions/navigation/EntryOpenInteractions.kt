package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryOpenInteraction {
    fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions = EntryOpenOptions())
    fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    ): PendingIntent
}

interface EntryContinueInteraction {
    suspend fun continueEntry(context: Context, entry: Entry): EntryChapter?
    suspend fun findNext(entry: Entry): EntryChapter?
}
