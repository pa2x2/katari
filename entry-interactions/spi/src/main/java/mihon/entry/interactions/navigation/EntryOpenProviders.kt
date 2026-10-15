package mihon.entry.interactions.navigation

import android.app.PendingIntent
import android.content.Context
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

interface EntryOpenProcessor {

    fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions)
    fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions): PendingIntent
}

interface EntryContinueProcessor {
    suspend fun findNext(entry: Entry): EntryChapter?
    suspend fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? = findNext(entry)
    fun open(context: Context, entry: Entry, chapter: EntryChapter)
}
