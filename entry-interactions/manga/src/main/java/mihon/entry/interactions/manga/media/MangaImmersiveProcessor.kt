package mihon.entry.interactions.manga.media

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.manga.state.mangaProgressState
import mihon.entry.interactions.manga.state.pageIndex
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.runtime.EntryImmersiveLoadMode
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaImmersiveProcessor(
    private val entryProgressRepository: EntryProgressRepository? = null,
    private val mediaSession: EntryMediaSessionProcessor,
    private val loadPageSession: suspend (Context, Entry, EntryChapter) -> ReaderChapter = { context, entry, chapter ->
        ReaderPageSessionLoader(context).load(entry, chapter)
    },
    private val now: () -> Long = System::currentTimeMillis,
) : EntryImmersiveProcessor {
    override val type: EntryType = EntryType.MANGA
    override val loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD
    override val preloadRadius: Int = 1
    private val persistMutex = Mutex()

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle {
        requireNotNull(chapter) { "Manga immersive loading requires a reading child" }
        val readerChapter = loadPageSession(context, entry, chapter)
        try {
            val pages = requireNotNull(readerChapter.pages)
            val progress = entryProgressRepository?.get(chapter.entryId, "", chapter.progressResourceKey)
            return EntryImmersiveHandle.ImagePages(
                entryType = type,
                chapterId = chapter.id,
                delegate = MangaImmersiveMedia(
                    readerChapter = readerChapter,
                    initialPageIndex = progress?.pageIndex?.toInt()?.coerceIn(0, pages.lastIndex) ?: 0,
                    entry = entry,
                    child = chapter,
                ),
            )
        } catch (e: Throwable) {
            readerChapter.unref()
            throw e
        }
    }

    override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
        val pages = handle as? EntryImmersiveHandle.ImagePages
            ?: error("Manga immersive feed received non-image media")
        val media = pages.delegate as? MangaImmersiveMedia
            ?: error("Manga immersive feed image media is unavailable")
        return MangaImmersiveRenderer(media)
    }

    override suspend fun persistProgress(
        handle: EntryImmersiveHandle,
        progress: EntryImmersiveProgress,
    ) {
        val pages = handle as? EntryImmersiveHandle.ImagePages ?: return
        val media = pages.delegate as? MangaImmersiveMedia ?: return
        val imageProgress = progress as? EntryImmersiveProgress.ImagePage ?: return
        if (imageProgress.pageCount <= 0) return

        persistMutex.withLock {
            val pageIndex = imageProgress.pageIndex.coerceIn(0, imageProgress.pageCount - 1)
            val completed = pageIndex == imageProgress.pageCount - 1
            val timestamp = now()
            mediaSession.onEvent(
                EntryMediaSessionEvent.Progressed(
                    visibleEntry = media.entry,
                    child = media.child,
                    progress = mangaProgressState(
                        entryId = media.child.entryId,
                        chapterId = media.child.id,
                        resourceKey = media.child.progressResourceKey,
                        pageIndex = pageIndex.toLong(),
                        pageCount = imageProgress.pageCount.toLong(),
                        completed = completed,
                        locatorUpdatedAt = timestamp,
                        completionUpdatedAt = if (completed) timestamp else 0L,
                    ),
                    fraction = pageIndex.toDouble() / imageProgress.pageCount,
                    completeEquivalentChildrenByNumber = true,
                    activity = imageProgress.sessionDurationMs.takeIf { it > 0L }?.let { duration ->
                        media.activitySession.record(
                            recordedAtEpochMillis = timestamp,
                            durationMillis = duration,
                        )
                    },
                ),
            )
        }
    }

    override fun release(handle: EntryImmersiveHandle) {
        val pages = handle as? EntryImmersiveHandle.ImagePages ?: return
        (pages.delegate as? MangaImmersiveMedia)?.readerChapter?.unref()
    }
}
