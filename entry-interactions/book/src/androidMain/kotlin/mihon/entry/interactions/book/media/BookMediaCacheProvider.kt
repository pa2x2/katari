package mihon.entry.interactions.book.media

import android.content.Context
import android.text.format.Formatter
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.media.EntryMediaCacheArtifact
import mihon.entry.interactions.media.EntryMediaCacheAutoClearPreference
import mihon.entry.interactions.media.EntryMediaCacheId
import mihon.entry.interactions.media.EntryMediaCacheProvider
import tachiyomi.i18n.*

internal class BookMediaCacheProvider(
    context: Context,
    cache: () -> BookMaterializationCache,
) : EntryMediaCacheProvider {
    override val artifacts: List<EntryMediaCacheArtifact> = listOf(BookMaterializationCacheArtifact(context, cache))
}

private class BookMaterializationCacheArtifact(
    private val context: Context,
    cache: () -> BookMaterializationCache,
) : EntryMediaCacheArtifact {
    private val delegate by lazy(cache)

    override val id = EntryMediaCacheId("book.materialized-resources")
    override val clearLabel = MR.strings.pref_clear_book_cache
    override val autoClearLabel = MR.strings.pref_auto_clear_book_cache
    override val autoClearPreference = EntryMediaCacheAutoClearPreference("auto_clear_book_materialization_cache")
    override val readableSize: String get() = Formatter.formatFileSize(context, delegate.sizeBytes)
    override fun clear(): Int = delegate.clear()
}
