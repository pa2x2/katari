package mihon.entry.interactions.anime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerActivity
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal fun animePlayerIntent(
    context: Context,
    entry: Entry,
    chapter: EntryChapter,
    options: EntryOpenOptions = EntryOpenOptions(),
): Intent {
    entry.requireAnime()
    return VideoPlayerActivity.newIntent(
        context = context,
        entry = entry,
        chapter = chapter,
        ownerEntryId = options.ownerEntryId ?: chapter.entryId,
        bypassMerge = options.bypassMerge,
    ).applyEntryOpenOptions(options)
}

internal fun animePlayerPendingIntent(
    context: Context,
    entry: Entry,
    chapter: EntryChapter,
    options: EntryOpenOptions = EntryOpenOptions(),
): PendingIntent {
    return PendingIntent.getActivity(
        context,
        chapter.id.hashCode(),
        animePlayerIntent(context, entry, chapter, options),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

internal fun openAnimeEpisode(
    context: Context,
    entry: Entry,
    chapter: EntryChapter,
    options: EntryOpenOptions = EntryOpenOptions(),
) {
    context.startActivity(
        animePlayerIntent(
            context = context,
            entry = entry,
            chapter = chapter,
            options = options,
        ),
    )
}

private fun Intent.applyEntryOpenOptions(options: EntryOpenOptions): Intent {
    if (options.newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (options.clearTop) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return this
}
