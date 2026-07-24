package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMediaCacheArtifact
import mihon.entry.interactions.EntryMediaCacheAutoClearPreference
import mihon.entry.interactions.EntryMediaCacheId
import mihon.entry.interactions.EntryMediaCacheProvider
import tachiyomi.i18n.MR

internal class BookMediaCacheProvider(
    cache: () -> BookMaterializationCache,
) : EntryMediaCacheProvider {
    override val type = EntryType.BOOK
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
    override val readableSize: String get() = delegate.readableSize
    override fun clear(): Int = delegate.clear()
}
