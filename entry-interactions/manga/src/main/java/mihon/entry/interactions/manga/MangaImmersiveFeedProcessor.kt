package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.EntryImmersiveFeedHandle
import mihon.entry.interactions.EntryImmersiveFeedProcessor
import mihon.entry.interactions.EntryImmersiveFeedProgress
import mihon.entry.interactions.EntryImmersiveFeedRenderer
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.EntryReaderTracking
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import java.util.Date

internal class MangaImmersiveFeedProcessor(
    private val entryChapterRepository: EntryChapterRepository? = null,
    private val historyRepository: HistoryRepository? = null,
    private val readerIncognitoState: EntryReaderIncognitoState? = null,
    private val readerTracking: EntryReaderTracking? = null,
) : EntryImmersiveFeedProcessor {
    override val type: EntryType = EntryType.MANGA
    private val persistMutex = Mutex()

    override fun isSupported(entry: Entry): Boolean = entry.type == type

    override fun preloadRadius(entryType: EntryType): Int = 1

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        source: UnifiedSource,
    ): EntryImmersiveFeedHandle {
        val imageSource = source as? EntryImageSource
            ?: error("Source ${source.name} does not support image pages")
        val media = source.getMedia(chapter.toSEntryChapter()) as? EntryMedia.ImagePages
            ?: error("Source ${source.name} did not return image pages")
        val pages = media.pages.mapIndexed { index, page ->
            val imageUrl = page.imageUrl ?: imageSource.getImageUrl(page)
            MangaImmersiveFeedPage(
                index = index,
                imageUrl = imageUrl,
                headers = imageSource.imageRequest(page, imageUrl).headers,
            )
        }
        if (pages.isEmpty() || pages.all { it.imageUrl.isBlank() }) {
            error("No pages found")
        }
        return EntryImmersiveFeedHandle.ImagePages(
            entryType = type,
            chapterId = chapter.id,
            delegate = MangaImmersiveFeedMedia(
                pages = pages,
                initialPageIndex = chapter.lastPageRead.toInt().coerceIn(0, pages.lastIndex),
                entryId = entry.id,
                sourceId = entry.source,
                chapterNumber = chapter.chapterNumber,
                context = context.applicationContext,
            ),
        )
    }

    override fun renderer(handle: EntryImmersiveFeedHandle): EntryImmersiveFeedRenderer {
        val pages = handle as? EntryImmersiveFeedHandle.ImagePages
            ?: error("Manga immersive feed received non-image media")
        val media = pages.delegate as? MangaImmersiveFeedMedia
            ?: error("Manga immersive feed image media is unavailable")
        return MangaImmersiveFeedRenderer(media)
    }

    override suspend fun persistProgress(
        handle: EntryImmersiveFeedHandle,
        progress: EntryImmersiveFeedProgress,
    ) {
        val pages = handle as? EntryImmersiveFeedHandle.ImagePages ?: return
        val media = pages.delegate as? MangaImmersiveFeedMedia ?: return
        val imageProgress = progress as? EntryImmersiveFeedProgress.ImagePage ?: return
        val repository = entryChapterRepository ?: return
        if (readerIncognitoState?.isIncognito(media.sourceId) == true) return
        if (imageProgress.pageCount <= 0) return

        persistMutex.withLock {
            val chapter = repository.getChapterById(handle.chapterId) ?: return@withLock
            val pageIndex = imageProgress.pageIndex.coerceIn(0, imageProgress.pageCount - 1)
            val completedNow = !chapter.read && pageIndex == imageProgress.pageCount - 1
            repository.update(
                chapter.copy(
                    read = chapter.read || completedNow,
                    lastPageRead = pageIndex.toLong(),
                ),
            )
            if (imageProgress.sessionDurationMs > 0L) {
                historyRepository?.upsertHistory(
                    HistoryUpdate(
                        chapterId = chapter.id,
                        readAt = Date(),
                        sessionReadDuration = imageProgress.sessionDurationMs,
                    ),
                )
            }
            if (completedNow && media.chapterNumber >= 0.0) {
                readerTracking?.updateChapterRead(media.context, media.entryId, media.chapterNumber)
            }
        }
    }

    override fun release(handle: EntryImmersiveFeedHandle) = Unit
}
