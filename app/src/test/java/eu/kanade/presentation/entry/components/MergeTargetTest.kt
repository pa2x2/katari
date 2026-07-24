package eu.kanade.presentation.entry.components

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceManager

class MergeTargetTest {

    @Test
    fun `build merge targets filters entries and merged members by requested type`() {
        val manga = entry(id = 1L, title = "Manga", type = EntryType.MANGA)
        val anime = entry(id = 2L, title = "Anime", type = EntryType.ANIME)
        val mixedMembers = entry(id = 3L, title = "Mixed members", type = EntryType.MANGA)
        val mangaMembers = entry(id = 4L, title = "Manga members", type = EntryType.MANGA)

        val targets = buildMergeTargets(
            libraryItems = listOf(
                libraryItem(manga),
                libraryItem(anime),
                libraryItem(mixedMembers, memberEntries = listOf(mixedMembers, anime)),
                libraryItem(
                    mangaMembers,
                    memberEntries = listOf(
                        mangaMembers,
                        entry(id = 5L, title = "Manga member", type = EntryType.MANGA),
                    ),
                ),
            ),
            sourceManager = FakeSourceManager,
            entryType = EntryType.MANGA,
        )

        targets.map { it.id } shouldContainExactly listOf(manga.id, mangaMembers.id)
    }
}

private object FakeSourceManager : SourceManager {
    override val isInitialized = MutableStateFlow(true)
    override val sources: Flow<List<UnifiedSource>> = flowOf(emptyList())

    override fun get(sourceKey: Long): UnifiedSource? = null
    override fun getOrStub(sourceKey: Long): UnifiedSource = error("Not used")
    override fun getAll(): List<UnifiedSource> = emptyList()
    override fun getStubSources(): List<UnifiedSource> = emptyList()
    override fun getDisplayInfo(sourceKey: Long): SourceDisplayInfo {
        return SourceDisplayInfo(
            id = sourceKey,
            name = "Source $sourceKey",
            lang = "en",
            isMissing = false,
        )
    }
}

private fun entry(
    id: Long,
    title: String,
    type: EntryType,
): Entry {
    return Entry.create().copy(
        id = id,
        source = 1L,
        title = title,
        type = type,
    )
}

private fun libraryItem(
    entry: Entry,
    memberEntries: List<Entry> = listOf(entry),
): LibraryItem {
    return LibraryItem(
        entry = entry,
        categories = emptyList(),
        sourceName = "Source",
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = entry.source,
        sourceIds = memberEntries.map { it.source }.toSet(),
        isLocal = false,
        isMerged = memberEntries.size > 1,
        memberEntryIds = memberEntries.map { LibraryItemKey(it.type, it.id) },
        memberEntries = memberEntries,
        progressSummary = EntryLibraryProgressResolution.Inapplicable(entry.type),
        latestUpload = 0L,
        downloadCount = 0,
    )
}
