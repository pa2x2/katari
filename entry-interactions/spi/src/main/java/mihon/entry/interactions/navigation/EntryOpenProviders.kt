package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import mihon.feature.graph.CapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryOpenProcessor : EntryInteractionProvider {

    fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions)
    fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions): PendingIntent
}

interface EntryContinueProcessor : EntryInteractionProvider {
    suspend fun findNext(entry: Entry): EntryChapter?
    fun open(context: Context, entry: Entry, chapter: EntryChapter)
}

val EntryOpenCapability = entryInteractionCapability<EntryOpenProcessor>(
    id = CapabilityId("entry.open"),
)

val EntryContinueCapability = entryInteractionCapability<EntryContinueProcessor>(
    id = CapabilityId("entry.continue"),
)
