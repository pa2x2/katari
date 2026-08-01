package eu.kanade.tachiyomi.data.backup.restore.entries

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalization
import org.junit.jupiter.api.Test

class EntryRestoreCoordinatorTest {

    @Test
    fun `failed batch retries individually without losing successful neighbors or duplicating progress`() = runTest {
        val store = RollbackEntryStore(failingTitle = "broken")
        val finalizedProfiles = mutableListOf<Long>()
        val entryFailures = mutableListOf<String>()
        val progress = mutableListOf<Pair<String, Int>>()
        val coordinator = coordinator(
            transaction = store::transaction,
            restoreEntry = store::restore,
            finalizeRestore = { profileId ->
                finalizedProfiles += profileId
                EntryBackupRestoreFinalization(emptyList())
            },
        )

        coordinator.restoreEntries(
            destinationProfileId = 7L,
            backupEntries = entries("before", "broken", "after"),
            backupCategories = emptyList(),
            onBatchFailure = {},
            onEntryFailure = { entry, _ -> entryFailures += entry.title },
            onProgress = { entry, count -> progress += entry.title to count },
        )

        store.persistedTitles shouldContainExactly listOf("before", "after")
        entryFailures shouldContainExactly listOf("broken")
        progress shouldContainExactly listOf("after" to 3)
        finalizedProfiles shouldContainExactly listOf(7L)
    }

    @Test
    fun `cancellation during individual retry is rethrown without progress or finalization`() = runTest {
        val cancellation = CancellationException("restore cancelled")
        val finalizedProfiles = mutableListOf<Long>()
        val progress = mutableListOf<Pair<String, Int>>()
        val coordinator = coordinator(
            transaction = { throw IllegalStateException("batch failed") },
            restoreEntry = { _, _ -> throw cancellation },
            finalizeRestore = { profileId ->
                finalizedProfiles += profileId
                EntryBackupRestoreFinalization(emptyList())
            },
        )

        val failure = runCatching {
            coordinator.restoreEntries(
                destinationProfileId = 7L,
                backupEntries = entries("cancelled"),
                backupCategories = emptyList(),
                onBatchFailure = {},
                onEntryFailure = { _, _ -> },
                onProgress = { entry, count -> progress += entry.title to count },
            )
        }.exceptionOrNull()

        failure shouldBe cancellation
        progress shouldBe emptyList()
        finalizedProfiles shouldBe emptyList()
    }

    private fun coordinator(
        transaction: suspend (block: suspend () -> Unit) -> Unit,
        restoreEntry: suspend (BackupEntry, List<BackupCategory>) -> Unit,
        finalizeRestore: suspend (Long) -> EntryBackupRestoreFinalization,
    ) = EntryRestoreCoordinator(
        transaction = transaction,
        sortByNew = { it },
        restoreEntry = restoreEntry,
        finalizeRestore = finalizeRestore,
    )

    private fun entries(vararg titles: String) = titles.mapIndexed { index, title ->
        BackupEntry(
            source = index.toLong(),
            url = "/$title",
            title = title,
        )
    }

    private class RollbackEntryStore(
        private val failingTitle: String,
    ) {
        val persistedTitles = mutableListOf<String>()

        suspend fun transaction(block: suspend () -> Unit) {
            val snapshot = persistedTitles.toList()
            try {
                block()
            } catch (e: Exception) {
                persistedTitles.clear()
                persistedTitles.addAll(snapshot)
                throw e
            }
        }

        suspend fun restore(entry: BackupEntry, @Suppress("UNUSED_PARAMETER") categories: List<BackupCategory>) {
            if (entry.title == failingTitle) {
                throw IllegalStateException("cannot restore ${entry.title}")
            }
            persistedTitles += entry.title
        }
    }
}
