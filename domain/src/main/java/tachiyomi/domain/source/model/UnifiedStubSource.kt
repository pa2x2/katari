package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource

/**
 * A [UnifiedSource] stub that mirrors missing source metadata.
 * All source operations throw [SourceNotInstalledException].
 */
class UnifiedStubSource(
    override val id: Long,
    val lang: String,
    override val name: String,
) : UnifiedSource {

    constructor(stub: StubSource) : this(
        id = stub.id,
        lang = stub.lang,
        name = stub.name,
    )

    override fun getFilterList(): EntryFilterList = EntryFilterList()

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        throw SourceNotInstalledException()

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        throw SourceNotInstalledException()

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = throw SourceNotInstalledException()

    override suspend fun getContentDetails(entry: SEntry): SEntry =
        throw SourceNotInstalledException()

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> =
        throw SourceNotInstalledException()

    override suspend fun getMedia(
        chapter: SEntryChapter,
        selection: PlaybackSelection,
    ): EntryMedia = throw SourceNotInstalledException()
}
