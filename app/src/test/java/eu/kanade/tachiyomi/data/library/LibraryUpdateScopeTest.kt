package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.ProgressState

class LibraryUpdateScopeTest {

    @Test
    fun `type scope matches only the requested entry type`() {
        libraryItem(EntryType.MANGA).matchesUpdateScope(-1L, -1L, EntryType.MANGA) shouldBe true
        libraryItem(EntryType.ANIME).matchesUpdateScope(-1L, -1L, EntryType.MANGA) shouldBe false
    }

    @Test
    fun `type category and source scopes are intersected`() {
        val item = libraryItem(
            type = EntryType.ANIME,
            categories = listOf(2L),
            sourceIds = setOf(10L, 11L),
        )

        item.matchesUpdateScope(2L, 11L, EntryType.ANIME) shouldBe true
        item.matchesUpdateScope(3L, 11L, EntryType.ANIME) shouldBe false
        item.matchesUpdateScope(2L, 12L, EntryType.ANIME) shouldBe false
        item.matchesUpdateScope(2L, 11L, EntryType.MANGA) shouldBe false
    }

    @Test
    fun `missing explicit scopes match every item`() {
        libraryItem(EntryType.MANGA).matchesUpdateScope(-1L, -1L, null) shouldBe true
        libraryItem(EntryType.ANIME).matchesUpdateScope(-1L, -1L, null) shouldBe true
    }
}

private fun libraryItem(
    type: EntryType,
    categories: List<Long> = listOf(0L),
    sourceIds: Set<Long> = setOf(1L),
): LibraryItem {
    val entry = Entry.create().copy(
        id = 1L,
        source = sourceIds.first(),
        favorite = true,
        title = "Entry",
        type = type,
    )
    return LibraryItem(
        entry = entry,
        categories = categories,
        sourceName = "Source",
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = entry.source,
        sourceIds = sourceIds,
        isLocal = false,
        isMerged = false,
        memberEntryIds = listOf(LibraryItemKey(type, entry.id)),
        memberEntries = listOf(entry),
        progress = ProgressState(
            totalCount = 0L,
            consumedCount = 0L,
            hasStarted = false,
        ),
        latestUpload = 0L,
        lastRead = 0L,
        continueEntryId = null,
        downloadCount = 0,
    )
}
