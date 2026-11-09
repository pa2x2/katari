package app.katari.entry.interactions.manga.media.immersive

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.manga.state.resumePageIndex
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.runtime.EntryImmersiveLoadMode
import app.katari.entry.viewer.settings.ViewerSettingBinding
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Manga's chapters in the immersive feed, alike on every platform: the chapter to read, loaded as the reader loads
 * it, from the page it was left at. A chapter in the feed is a preview: it follows the chapter's saved page and never
 * moves it, nor marks the chapter read. The feed's immersive processor is a platform facility, so each platform
 * answers it with its own `MangaImmersiveProcessor` over these.
 */
internal class MangaImmersiveChapters(
    private val entryProgressRepository: EntryProgressRepository,
    private val loadPageSession: suspend (PlatformContext, Entry, EntryChapter) -> ReaderChapter,
    private val bindKeepScreenOn: (entryId: Long) -> ViewerSettingBinding<Boolean>,
    private val platform: MangaImmersivePlatform,
) {
    val loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD

    // Two, as clips, so skipping quickly through chapters finds the next one open
    val preloadAhead: Int = 2

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
            val progress = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
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
        return MangaImmersiveRenderer(
            media = media,
            savedPageIndex = savedPageIndex(media.child),
            keepScreenOn = bindKeepScreenOn(media.entry.id),
            platform = platform,
        )
    }

    /** The page the chapter resumes at, as the reader and other devices save it. */
    private fun savedPageIndex(child: EntryChapter): Flow<Int> =
        entryProgressRepository.getByChapterIdAsFlow(child.id)
            .map { states ->
                states.firstOrNull { it.contentKey.isEmpty() && it.resourceKey == child.progressResourceKey }
                    ?.resumePageIndex
                    ?.toInt()
                    ?: 0
            }
            .distinctUntilChanged()

    fun release(handle: EntryImmersiveHandle) {
        val pages = handle as? EntryImmersiveHandle.ImagePages ?: return
        (pages.delegate as? MangaImmersiveMedia)?.readerChapter?.unref()
    }
}
