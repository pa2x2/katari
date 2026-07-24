package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMediaCacheArtifact
import mihon.entry.interactions.EntryMediaCacheAutoClearPreference
import mihon.entry.interactions.EntryMediaCacheId
import mihon.entry.interactions.EntryMediaCacheProvider
import mihon.entry.interactions.EntryPageImageCache
import tachiyomi.i18n.MR

internal class MangaMediaCacheProvider(
    pageImageCache: () -> EntryPageImageCache,
) : EntryMediaCacheProvider {
    override val type = EntryType.MANGA
    override val artifacts: List<EntryMediaCacheArtifact> = listOf(MangaPageImageCacheArtifact(pageImageCache))
}

private class MangaPageImageCacheArtifact(
    cache: () -> EntryPageImageCache,
) : EntryMediaCacheArtifact {
    private val delegate by lazy(cache)

    override val id = EntryMediaCacheId("manga.page-images")
    override val clearLabel = MR.strings.pref_clear_chapter_cache
    override val autoClearLabel = MR.strings.pref_auto_clear_chapter_cache
    override val autoClearPreference = EntryMediaCacheAutoClearPreference("auto_clear_chapter_cache")
    override val readableSize: String get() = delegate.readableSize
    override fun clear(): Int = delegate.clear()
}
