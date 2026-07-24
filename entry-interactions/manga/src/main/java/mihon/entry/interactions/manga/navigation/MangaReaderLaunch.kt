package mihon.entry.interactions.manga

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal fun mangaReaderIntent(
    context: Context,
    entry: Entry,
    chapter: EntryChapter,
    options: EntryOpenOptions = EntryOpenOptions(),
): Intent {
    entry.requireManga()
    return ReaderActivity.newIntent(context, entry, chapter, options.pageIndex)
        .applyEntryOpenOptions(options)
}

internal fun mangaReaderPendingIntent(
    context: Context,
    entry: Entry,
    chapter: EntryChapter,
    options: EntryOpenOptions = EntryOpenOptions(),
): PendingIntent {
    return PendingIntent.getActivity(
        context,
        chapter.id.hashCode(),
        mangaReaderIntent(context, entry, chapter, options),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

internal fun openMangaChapter(
    context: Context,
    entry: Entry,
    chapter: EntryChapter,
    options: EntryOpenOptions = EntryOpenOptions(),
) {
    context.startActivity(mangaReaderIntent(context, entry, chapter, options))
}

private fun Intent.applyEntryOpenOptions(options: EntryOpenOptions): Intent {
    if (options.newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (options.clearTop) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return this
}
