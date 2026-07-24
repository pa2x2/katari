package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry

class EntryLibraryMembershipCoordinatorTest {

    @Test
    fun `category choice does not trigger persistence or after-commit work`() = runTest {
        val trace = mutableListOf<String>()
        val host = RecordingHost(trace, defaultCategoryId = -1, categories = listOf(Category(7L, "Shelf", 0L, 0L)))
        val feature = feature(
            host = host,
            trace = trace,
        )

        val result = feature.add(EntryLibraryAddRequest(entry()))

        (result is EntryLibraryAddResult.CategorySelectionRequired) shouldBe true
        host.addCalls shouldBe 0
        trace shouldContainExactly emptyList()
    }

    @Test
    fun `addition consequence runs only after persistence commits`() = runTest {
        val trace = mutableListOf<String>()
        val host = RecordingHost(trace)
        val feature = feature(host, trace = trace)

        val result = feature.add(EntryLibraryAddRequest(entry()))

        check(result is EntryLibraryAddResult.Added) { "Unexpected addition result: $result" }
        trace shouldContainExactly listOf("membership-committed", "addition-consequence")
    }

    @Test
    fun `addition conflict suppresses volatile consequence`() = runTest {
        val trace = mutableListOf<String>()
        val host = RecordingHost(trace, additionConflicts = true)
        val feature = feature(host, trace = trace)

        val result = feature.add(EntryLibraryAddRequest(entry()))

        (result is EntryLibraryAddResult.Failed) shouldBe true
        trace shouldContainExactly emptyList()
    }

    @Test
    fun `transactional removal consequence failure prevents membership commit`() = runTest {
        val trace = mutableListOf<String>()
        val host = RecordingHost(trace)
        val feature = feature(
            host = host,
            trace = trace,
            removingFailure = IllegalStateException("merge conflict"),
        )

        val result = feature.remove(listOf(entry().copy(favorite = true)))

        (result is EntryLibraryRemovalResult.Failed) shouldBe true
        host.removeCommitted shouldBe false
        trace shouldContainExactly listOf("removing-consequence")
    }

    private fun feature(
        host: RecordingHost,
        trace: MutableList<String>,
        removingFailure: Throwable? = null,
    ): EntryLibraryMembershipFeature {
        val type = EntryType.entries.first()
        val plugin = object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("library-membership-test-type")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(
                EntryLibraryMembershipFeatureContributor,
                EntryTrackingLibraryMembershipContributor,
                EntryMergeLibraryMembershipContributor,
                EntryDownloadLibraryMembershipContributor,
                EntryLibraryCustomCoverContributor,
            ),
            executionBindings = listOf(
                FeatureExecutionParticipantBinding(
                    ENTRY_TRACKING_LIBRARY_ADDITION_PARTICIPANT,
                    FeatureExecutionHandler { trace += "addition-consequence" },
                ),
                FeatureExecutionParticipantBinding(
                    ENTRY_MERGE_LIBRARY_REMOVAL_PARTICIPANT,
                    FeatureExecutionHandler {
                        trace += "removing-consequence"
                        removingFailure?.let { throw it }
                    },
                ),
                FeatureExecutionParticipantBinding(
                    ENTRY_DOWNLOAD_LIBRARY_REMOVAL_PARTICIPANT,
                    FeatureExecutionHandler { },
                ),
                FeatureExecutionParticipantBinding(
                    ENTRY_LIBRARY_CUSTOM_COVER_REMOVAL_PARTICIPANT,
                    FeatureExecutionHandler { },
                ),
            ),
        )
        return EntryLibraryMembershipCoordinator(
            host = host,
            mergeCandidates = mockk { coEvery { candidates(any()) } returns emptyList() },
            executions = composition.featureExecutions,
        )
    }

    private fun entry() = Entry.create().copy(
        id = 44L,
        favorite = false,
        type = EntryType.entries.first(),
    )
}

private class RecordingHost(
    private val trace: MutableList<String>,
    private val defaultCategoryId: Long = 0L,
    private val categories: List<Category> = emptyList(),
    private val additionConflicts: Boolean = false,
) : EntryLibraryMembershipHost {
    var addCalls = 0
    var removeCommitted = false

    override suspend fun prepareAddition(entry: Entry) = EntryLibraryMembershipPreparation(
        categories = categories,
        defaultCategoryId = defaultCategoryId,
        selectedCategoryIds = emptySet(),
        defaultChildFlags = 7L,
    )

    override suspend fun add(
        entry: Entry,
        categoryIds: List<Long>,
        defaultChildFlags: Long,
    ): EntryLibraryMembershipCommit {
        addCalls++
        if (additionConflicts) return EntryLibraryMembershipCommit.Conflict
        trace += "membership-committed"
        return EntryLibraryMembershipCommit.Applied(listOf(entry.copy(favorite = true)))
    }

    override suspend fun remove(
        entries: List<Entry>,
        beforeCommit: suspend (persistedEntries: List<Entry>) -> Unit,
    ): EntryLibraryMembershipCommit {
        beforeCommit(entries)
        trace += "membership-removed"
        removeCommitted = true
        return EntryLibraryMembershipCommit.Applied(entries.map { it.copy(favorite = false) })
    }
}
