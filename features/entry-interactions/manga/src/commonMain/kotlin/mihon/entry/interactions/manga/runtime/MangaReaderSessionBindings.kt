package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.runtime.EntryTypeKey

/** What a reading session reports to and reads from besides its pages: the media session and stored translations. */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaReaderSessionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun chapterTranslationStore(provider: DownloadProvider): MangaChapterTranslationStore =
        MangaChapterTranslationStore(provider)

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSession(sink: EntryMediaSessionEventSink): MangaMediaSessionProcessor =
        MangaMediaSessionProcessor(sink)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun mediaSessionProcessor(processor: MangaMediaSessionProcessor): EntryMediaSessionProcessor = processor
}
