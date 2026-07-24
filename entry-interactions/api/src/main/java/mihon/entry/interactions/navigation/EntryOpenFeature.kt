package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned gate for every shared way of opening an Entry child. */
interface EntryOpenFeature {
    fun isApplicable(type: EntryType): Boolean

    /** Opens [chapter] when the Entry type contributes Open behavior. */
    fun open(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    ): Boolean

    /** Creates an Open action only when the Entry type contributes Open behavior. */
    fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    ): PendingIntent?
}
