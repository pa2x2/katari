package app.katari.entry.interactions.manga.media.immersive

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.manga.state.mangaProgressState
import app.katari.entry.interactions.manga.state.resumePageIndex
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveProgress
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.media.EntryMediaSessionProcessor
import app.katari.entry.interactions.media.session.EntryMediaSessionEvent
import app.katari.entry.interactions.runtime.EntryImmersiveLoadMode
import app.katari.entry.viewer.settings.ViewerSettingBinding
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Manga's chapters in the immersive feed, alike on every platform: the chapter to read, loaded as the reader loads
 * it, from the page it was left at, with its progress saved as the reader saves it. The feed's immersive processor is
 * a platform facility, so each platform answers it with its own `MangaImmersiveProcessor` over these.
 */
internal class MangaImmersiveChapters(
    private val entryProgressRepository: EntryProgressRepository? = null,
    private val mediaSession: EntryMediaSessionProcessor,
    private val loadPageSession: suspend (PlatformContext, Entry, EntryChapter) -> ReaderChapter,
    private val bindKeepScreenOn: (entryId: Long) -> ViewerSettingBinding<Boolean>,
    private val platform: MangaImmersivePlatform,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD
    val preloadRadius: Int = 1
    private val persistMutex = Mutex()

    suspend fun load(
        context: PlatformContext,
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
                entryType = EntryType.MANGA,
                chapterId = chapter.id,
                delegate = MangaImmersiveMedia(
                    readerChapter = readerChapter,
                    initialPageIndex = progress?.resumePageIndex?.toInt()?.coerceIn(0, pages.lastIndex) ?: 0,
                    entry = entry,
                    child = chapter,
                ),
            )
        } catch (e: Throwable) {
            readerChapter.unref()
            throw e
        }
    }

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
        val pages = handle as? EntryImmersiveHandle.ImagePages
            ?: error("Manga immersive feed received non-image media")
        val media = pages.delegate as? MangaImmersiveMedia
            ?: error("Manga immersive feed image media is unavailable")
        return MangaImmersiveRenderer(media, keepScreenOn = bindKeepScreenOn(media.entry.id), platform = platform)
    }

    suspend fun persistProgress(
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

    fun release(handle: EntryImmersiveHandle) {
        val pages = handle as? EntryImmersiveHandle.ImagePages ?: return
        (pages.delegate as? MangaImmersiveMedia)?.readerChapter?.unref()
    }
}
