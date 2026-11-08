package mihon.entry.interactions.manga.media

import mihon.entry.interactions.media.EntryMediaCacheArtifact
import mihon.entry.interactions.media.EntryMediaCacheProvider

/** The desktop's manga media caches: the shared [MangaPageImageCache]. */
internal class MangaMediaCacheProvider(cache: MangaPageImageCache) : EntryMediaCacheProvider {
    override val artifacts: List<EntryMediaCacheArtifact> = listOf(MangaPageImageCacheArtifact(cache))
}

private class MangaPageImageCacheArtifact(private val cache: MangaPageImageCache) : EntryMediaCacheArtifact {
    override val id get() = cache.id
    override val clearLabel get() = cache.clearLabel
    override val autoClearLabel get() = cache.autoClearLabel
    override val autoClearPreference get() = cache.autoClearPreference
    override val sizeBytes get() = cache.sizeBytes

    override fun clear() = cache.clear()
}
