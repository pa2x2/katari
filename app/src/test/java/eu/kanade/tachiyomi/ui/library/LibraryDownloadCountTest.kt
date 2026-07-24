package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.EntryDownloadRuntimeFeature
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryDownloadCountTest {

    @Test
    fun `merged download count uses each member's source and title`() {
        val first = entry(id = 1L, source = 10L, title = "First")
        val second = entry(id = 2L, source = 20L, title = "Second")
        val downloads = mockk<EntryDownloadRuntimeFeature> {
            every { downloadCount(first) } returns 2
            every { downloadCount(second) } returns 3
        }
        val item = libraryItem(first, second)

        item.calculateDownloadCount(downloads) shouldBe 5
    }

    private fun entry(id: Long, source: Long, title: String): Entry = Entry.create().copy(
        id = id,
        source = source,
        title = title,
        type = EntryType.MANGA,
    )

    private fun libraryItem(vararg entries: Entry): LibraryItem = LibraryItem(
        entry = entries.first(),
        categories = emptyList(),
        sourceName = "",
        sourceLanguage = "",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = LibraryItem.MULTI_SOURCE_ID,
        sourceIds = entries.mapTo(mutableSetOf(), Entry::source),
        isLocal = false,
        isMerged = true,
        memberEntryIds = entries.map { LibraryItemKey(it.type, it.id) },
        memberEntries = entries.toList(),
        progressSummary = EntryLibraryProgressResolution.Inapplicable(entries.first().type),
        latestUpload = 0L,
        downloadCount = 0,
    )
}
