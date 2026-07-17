package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.ProgressState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class LibrarySelectionActionTest {

    @Test
    fun `selection actions preserve distinct merged member ids`() {
        selectedActionEntryIds(
            listOf(
                libraryItem(id = 1L, memberIds = listOf(1L, 2L)),
                libraryItem(id = 2L),
                libraryItem(id = 3L),
            ),
        ) shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun `merged item categories include every member category`() = runTest {
        val categories = mapOf(
            1L to listOf(category(10L)),
            2L to listOf(category(20L), category(10L)),
        )

        categoriesForLibraryItem(
            item = libraryItem(id = 1L, memberIds = listOf(1L, 2L)),
            getCategories = { categories[it].orEmpty() },
        ).map(Category::id) shouldBe listOf(10L, 20L)
    }

    @Test
    fun `category actions update every distinct merged member`() = runTest {
        val currentCategories = mapOf(
            1L to emptyList(),
            2L to emptyList(),
            3L to listOf(20L),
        )
        val updates = mutableMapOf<Long, List<Long>>()

        updateLibraryItemCategories(
            items = listOf(
                libraryItem(id = 1L, memberIds = listOf(1L, 2L)),
                libraryItem(id = 2L),
                libraryItem(id = 3L),
            ),
            addCategories = listOf(10L),
            removeCategories = listOf(20L),
            getCategoryIds = { currentCategories[it].orEmpty() },
            setCategoryIds = { entryId, categoryIds -> updates[entryId] = categoryIds },
        )

        updates shouldBe mapOf(
            1L to listOf(10L),
            2L to listOf(10L),
            3L to listOf(10L),
        )
    }

    @Test
    fun `selection is captured before asynchronous actions clear it`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryScreenModel.kt")
            .readText()

        assertTrue(
            source.contains(
                """
                fun performDownloadAction(action: DownloadAction) {
                        val entryIds = selectedActionEntryIds(state.value.selectedLibraryItems)
                        downloadBulkDownloadCandidates(action, entryIds)
                        clearSelection()
                    }
                """.trimIndent(),
            ),
        )
        assertTrue(
            source.contains(
                """
                fun markReadSelection(read: Boolean) {
                        val entryIds = selectedActionEntryIds(state.value.selectedLibraryItems)
                        screenModelScope.launchNonCancellable {
                            val entries = getActionEntries(entryIds)
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `dialog and move actions capture selection before dispatch`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryScreenModel.kt")
            .readText()

        listOf(
            """
            fun openChangeCategoryDialog() {
                    val state = state.value
                    val items = state.selection.mapNotNull { state.libraryData.favoritesById[it] }
                    // Hide the default category because it has a different behavior than the ones from db.
                    val categories = state.libraryData.categories.filter { it.id != 0L }
                    screenModelScope.launchIO {
            """.trimIndent(),
            """
            fun openDeleteEntriesDialog() {
                    val selectedItems = state.value.selectedLibraryItems
                    val entryIds = selectedActionEntryIds(selectedItems)
                    val containsMergedEntries = selectedItems.any(LibraryItem::isMerged)
                    screenModelScope.launchIO {
            """.trimIndent(),
            """
            fun prepareMoveToProfile(profile: Profile, destinationCategoryId: Long?) {
                    if (moveInProgress) return
                    val sourceProfileId = profileStore.currentProfileId
                    val selectedIds = state.value.selectedLibraryItems.map { it.entry.id }.distinct()
            """.trimIndent(),
            """
            fun openMergeDialog() {
                    val selectedItems = state.value.selectedLibraryItems
                    if (!entryCapabilityInteraction.canMergeSelection(selectedItems.toEntryMergeCapabilityItems())) return
                    screenModelScope.launchIO {
            """.trimIndent(),
        ).forEach { expected -> assertTrue(source.contains(expected)) }
    }

    private fun repositoryRoot(): Path {
        return generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }
    }
}

private fun libraryItem(id: Long, memberIds: List<Long> = listOf(id)): LibraryItem {
    val entry = Entry.create().copy(
        id = id,
        source = 1L,
        favorite = true,
        title = "Entry $id",
        type = EntryType.ANIME,
    )
    return LibraryItem(
        entry = entry,
        categories = listOf(0L),
        sourceName = "Source",
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = 1L,
        sourceIds = setOf(1L),
        isLocal = false,
        isMerged = memberIds.size > 1,
        memberEntryIds = memberIds.map { LibraryItemKey(EntryType.ANIME, it) },
        memberEntries = listOf(entry),
        progress = ProgressState(
            totalCount = 0L,
            consumedCount = 0L,
            inProgressItemId = null,
            inProgressFraction = null,
            hasStarted = false,
            continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
        ),
        latestUpload = 0L,
        lastRead = 0L,
        continueEntryId = null,
        downloadCount = 0,
    )
}

private fun category(id: Long): Category {
    return Category(id = id, name = "Category $id", order = id, flags = 0L)
}
