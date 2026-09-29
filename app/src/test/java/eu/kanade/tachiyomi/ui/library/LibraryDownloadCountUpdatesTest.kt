package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryDownloadCountUpdatesTest {

    @Test
    fun `member updates accumulate counts without copying unaffected items`() = runTest {
        val unaffected = libraryItem(entry(id = 1L, type = EntryType.ANIME))
        val firstMember = entry(id = 2L)
        val secondMember = entry(id = 3L)
        val affected = libraryItem(firstMember, secondMember)
        val trailing = libraryItem(entry(id = 4L))
        val counts = mutableMapOf(2L to 0, 3L to 0)
        val statuses = MutableSharedFlow<EntryDownloadStatus>()
        val emissions = mutableListOf<List<LibraryItem>>()
        val collection = launch(start = CoroutineStart.UNDISPATCHED) {
            observeLibraryDownloadCountUpdates(
                initialItems = listOf(unaffected, affected, trailing),
                statusUpdates = statuses,
                calculateDownloadCount = { item -> item.memberEntries.sumOf { counts[it.id] ?: 0 } },
            ).toList(emissions)
        }

        counts[3L] = 2
        statuses.emit(status(entryId = 3L))
        runCurrent()
        counts[2L] = 1
        statuses.emit(status(entryId = 2L))
        runCurrent()

        emissions.map { items -> items.map(LibraryItem::downloadCount) } shouldContainExactly listOf(
            listOf(0, 0, 0),
            listOf(0, 2, 0),
            listOf(0, 3, 0),
        )
        emissions.last().map { it.entry.id } shouldContainExactly listOf(1L, 2L, 4L)
        assertSame(unaffected, emissions.last()[0])
        assertSame(trailing, emissions.last()[2])
        collection.cancelAndJoin()
    }

    private fun status(entryId: Long) = EntryDownloadStatus(
        entryType = EntryType.MANGA,
        chapterId = entryId * 10,
        state = EntryDownloadState.DOWNLOADED,
        entryId = entryId,
        persistedContentChanged = true,
    )

    private fun entry(
        id: Long,
        source: Long = id,
        title: String = "Entry $id",
        type: EntryType = EntryType.MANGA,
    ): Entry = Entry.create().copy(
        id = id,
        source = source,
        title = title,
        type = type,
    )

    private fun libraryItem(vararg entries: Entry): LibraryItem = LibraryItem(
        entry = entries.first(),
        categories = emptyList(),
        sourceName = "",
        sourceLanguage = "",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = entries.singleOrNull()?.source ?: LibraryItem.MULTI_SOURCE_ID,
        sourceIds = entries.mapTo(mutableSetOf(), Entry::source),
        isLocal = false,
        isMerged = entries.size > 1,
        memberEntryIds = entries.map { LibraryItemKey(it.type, it.id) },
        memberEntries = entries.toList(),
        progressSummary = EntryLibraryProgressResolution.Inapplicable(entries.first().type),
        latestUpload = 0L,
        downloadCount = 0,
    )
}
