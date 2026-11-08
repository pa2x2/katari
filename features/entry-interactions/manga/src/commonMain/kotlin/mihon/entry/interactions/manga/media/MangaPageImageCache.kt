package mihon.entry.interactions.manga.media

import mihon.entry.interactions.media.EntryMediaCacheAutoClearPreference
import mihon.entry.interactions.media.EntryMediaCacheId
import mihon.entry.interactions.runtime.EntryPageImageCache
import tachiyomi.i18n.*

/**
 * The cache of chapter page images the reader, previews and the immersive feed fill, as the settings list it among the
 * media caches. The cache artifact is a platform facility, so each platform's `MangaMediaCacheProvider` answers it over
 * this, which keeps its id and preference the same everywhere.
 */
internal class MangaPageImageCache(cache: () -> EntryPageImageCache) {
    private val delegate by lazy(cache)

    val id = EntryMediaCacheId("manga.page-images")
    val clearLabel = MR.strings.pref_clear_chapter_cache
    val autoClearLabel = MR.strings.pref_auto_clear_chapter_cache
    val autoClearPreference = EntryMediaCacheAutoClearPreference("auto_clear_chapter_cache")
    val sizeBytes: Long get() = delegate.sizeBytes

    fun clear(): Int = delegate.clear()
}
