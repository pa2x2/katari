package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.ProgressState

class LibraryScreenModelMergeDialogTest {

    @Test
    fun `anime-only merge selection builds anime dialog entries`() {
        val dialog = buildMergeDialog(
            listOf(
                libraryItem(id = 1L, title = "Anime 1", type = EntryType.ANIME),
                libraryItem(id = 2L, title = "Anime 2", type = EntryType.ANIME),
            ),
        ) ?: error("Expected merge dialog")

        dialog.entries.map { it.id to it.entry.type } shouldBe listOf(
            1L to EntryType.ANIME,
            2L to EntryType.ANIME,
        )
        dialog.targetId shouldBe 1L
    }

    @Test
    fun `mixed merge selection is rejected`() {
        buildMergeDialog(
            listOf(
                libraryItem(id = 1L, title = "Manga", type = EntryType.MANGA),
                libraryItem(id = 2L, title = "Anime", type = EntryType.ANIME),
            ),
        ) shouldBe null
    }

    @Test
    fun `merged anime selection preserves member entries`() {
        val dialog = buildMergeDialog(
            listOf(
                libraryItem(
                    id = 1L,
                    title = "Root",
                    type = EntryType.ANIME,
                    isMerged = true,
                    memberEntries = listOf(
                        entry(id = 1L, title = "Root", type = EntryType.ANIME),
                        entry(id = 2L, title = "Member", type = EntryType.ANIME),
                    ),
                ),
                libraryItem(id = 3L, title = "New", type = EntryType.ANIME),
            ),
        ) ?: error("Expected merge dialog")

        dialog.entries.map { it.entry.title to it.entry.type } shouldBe listOf(
            "New" to EntryType.ANIME,
            "Root" to EntryType.ANIME,
            "Member" to EntryType.ANIME,
        )
    }
}

private fun libraryItem(
    id: Long,
    title: String,
    type: EntryType,
    isMerged: Boolean = false,
    memberEntries: List<Entry> = listOf(entry(id = id, title = title, type = type)),
): LibraryItem {
    return LibraryItem(
        entry = entry(id = id, title = title, type = type),
        categories = listOf(0L),
        sourceName = "Source",
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = 1L,
        sourceIds = setOf(1L),
        isLocal = false,
        isMerged = isMerged,
        memberEntryIds = memberEntries.map { LibraryItemKey(it.type, it.id) },
        memberEntries = memberEntries,
        progress = when (type) {
            EntryType.MANGA -> ProgressState(
                totalCount = 0L,
                consumedCount = 0L,
                bookmarkCount = 0L,
                hasStarted = false,
            )
            EntryType.ANIME -> ProgressState(
                totalCount = 0L,
                consumedCount = 0L,
                inProgressItemId = null,
                inProgressFraction = null,
                hasStarted = false,
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            )
        },
        latestUpload = 0L,
        lastRead = 0L,
        continueEntryId = null,
        downloadCount = 0,
    )
}

private fun entry(id: Long, title: String, type: EntryType): Entry {
    return Entry.create().copy(
        id = id,
        source = 1L,
        favorite = true,
        title = title,
        type = type,
    )
}
