package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.EntryImmersiveHandle
import mihon.entry.interactions.EntryImmersiveLoadMode
import mihon.entry.interactions.EntryImmersiveProcessor
import mihon.entry.interactions.EntryImmersiveProgress
import mihon.entry.interactions.EntryImmersiveRenderer
import mihon.entry.interactions.EntryMediaSessionActivity
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionProcessor
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaImmersiveProcessor(
    private val entryProgressRepository: EntryProgressRepository? = null,
    private val mediaSession: EntryMediaSessionProcessor,
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
        val imageSource = source as? EntryImageSource
            ?: error("Source ${source.name} does not support image pages")
        val media = source.getMedia(chapter.toSEntryChapter()) as? EntryMedia.ImagePages
            ?: error("Source ${source.name} did not return image pages")
        val pages = media.pages.mapIndexed { index, page ->
            val imageUrl = page.imageUrl ?: imageSource.getImageUrl(page)
            MangaImmersivePage(
                index = index,
                imageUrl = imageUrl,
                headers = imageSource.imageRequest(page, imageUrl).headers,
            )
        }
        if (pages.isEmpty() || pages.all { it.imageUrl.isBlank() }) {
            error("No pages found")
        }
        val progress = entryProgressRepository?.get(chapter.entryId, "", chapter.progressResourceKey)
        return EntryImmersiveHandle.ImagePages(
            entryType = type,
            chapterId = chapter.id,
            delegate = MangaImmersiveMedia(
                pages = pages,
                initialPageIndex = progress?.pageIndex?.toInt()?.coerceIn(0, pages.lastIndex) ?: 0,
                entry = entry,
                child = chapter,
            ),
        )
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
                        EntryMediaSessionActivity(
                            recordedAtEpochMillis = timestamp,
                            durationMillis = duration,
                        )
                    },
                ),
            )
        }
    }

    override fun release(handle: EntryImmersiveHandle) = Unit
}
