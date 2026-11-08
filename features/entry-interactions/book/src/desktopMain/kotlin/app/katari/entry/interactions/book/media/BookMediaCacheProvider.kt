package app.katari.entry.interactions.book.media

import app.katari.entry.interactions.book.content.BookMaterializationCache
import app.katari.entry.interactions.media.EntryMediaCacheArtifact
import app.katari.entry.interactions.media.EntryMediaCacheAutoClearPreference
import app.katari.entry.interactions.media.EntryMediaCacheId
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import tachiyomi.i18n.*

/** The desktop's book media caches: the materialized resources the reader opens books from. */
internal class BookMediaCacheProvider(
    cache: () -> BookMaterializationCache,
) : EntryMediaCacheProvider {
    override val artifacts: List<EntryMediaCacheArtifact> = listOf(BookMaterializationCacheArtifact(cache))
}

private class BookMaterializationCacheArtifact(
    cache: () -> BookMaterializationCache,
) : EntryMediaCacheArtifact {
    private val delegate by lazy(cache)

    override val id = EntryMediaCacheId("book.materialized-resources")
    override val clearLabel = MR.strings.pref_clear_book_cache
    override val autoClearLabel = MR.strings.pref_auto_clear_book_cache
    override val autoClearPreference = EntryMediaCacheAutoClearPreference("auto_clear_book_materialization_cache")
    override val sizeBytes: Long get() = delegate.sizeBytes
    override fun clear(): Int = delegate.clear()
}
