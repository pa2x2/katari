package mihon.entry.interactions.manga

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryOpenProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class MangaOpenProcessor(
    private val openChapter: (Context, Entry, EntryChapter, EntryOpenOptions) -> Unit = ::openMangaChapter,
    private val createPendingIntent: (Context, Entry, EntryChapter, EntryOpenOptions) -> PendingIntent =
        ::mangaReaderPendingIntent,
) : EntryOpenProcessor {
    override val type: EntryType = EntryType.MANGA

    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireManga()
        openChapter(context, entry, chapter, options)
    }

    override fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): PendingIntent {
        entry.requireManga()
        return createPendingIntent(context, entry, chapter, options)
    }
}
