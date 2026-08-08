package mihon.entry.interactions.navigation

import android.app.PendingIntent
import android.content.Context
import mihon.entry.interactions.runtime.EntryInteractionProvider
import mihon.entry.interactions.runtime.entryInteractionCapability
import mihon.feature.graph.CapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

interface EntryOpenProcessor : EntryInteractionProvider {

    fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions)
    fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions): PendingIntent
}

interface EntryContinueProcessor : EntryInteractionProvider {
    suspend fun findNext(entry: Entry): EntryChapter?
    suspend fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? = findNext(entry)
    fun open(context: Context, entry: Entry, chapter: EntryChapter)
}

val EntryOpenCapability = entryInteractionCapability<EntryOpenProcessor>(
    id = CapabilityId("entry.open"),
)

val EntryContinueCapability = entryInteractionCapability<EntryContinueProcessor>(
    id = CapabilityId("entry.continue"),
)
