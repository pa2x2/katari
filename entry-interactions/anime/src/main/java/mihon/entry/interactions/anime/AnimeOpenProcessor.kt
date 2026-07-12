package mihon.entry.interactions.anime

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryOpenProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class AnimeOpenProcessor(
    private val openEpisode: (Context, Entry, EntryChapter, EntryOpenOptions) -> Unit = ::openAnimeEpisode,
    private val createPendingIntent: (Context, Entry, EntryChapter, EntryOpenOptions) -> PendingIntent =
        ::animePlayerPendingIntent,
) : EntryOpenProcessor {
    override val type: EntryType = EntryType.ANIME

    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        entry.requireAnime()
        openEpisode(context, entry, chapter, options)
    }

    override fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): PendingIntent {
        entry.requireAnime()
        return createPendingIntent(context, entry, chapter, options)
    }
}
