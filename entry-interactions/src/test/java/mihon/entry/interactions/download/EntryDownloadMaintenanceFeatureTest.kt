package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.EntryMergeDownloadOwners
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryDownloadMaintenanceFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `removal stays incomplete when deletion fails or downloads remain`() = runTest {
        val failedDeletion = entry
        val remainingDownloads = entry.copy(id = 8L, url = "/member")
        val processor = processor()
        // A failed deletion can evict the cache, so its owner no longer reports downloads afterwards.
        every { processor.hasDownloads(failedDeletion) } returnsMany listOf(true, false)
        coEvery { processor.deleteEntryDownloads(failedDeletion) } returns false
        every { processor.hasDownloads(remainingDownloads) } returns true
        val feature = featureFor(
            EntryDownloadCapability.bind(processor),
            owners = listOf(failedDeletion, remainingDownloads),
        )
        val plan = (feature.prepareRemoval(entry) as EntryDownloadRemovalPreparation.Prepared).plan

        feature.applyRemoval(plan) shouldBe
            EntryDownloadMaintenanceResult.Incomplete(listOf(failedDeletion, remainingDownloads))
    }

    private fun featureFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
        owners: List<Entry> = listOf(entry),
    ): EntryDownloadMaintenanceFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(*bindings)),
            featureContributors = listOf(EntryDownloadMaintenanceFeatureContributor),
        )
        return DefaultEntryDownloadMaintenanceFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.download,
            ownership = mockk {
                coEvery { resolveDownloadOwners(any()) } returns EntryMergeDownloadOwners(
                    profileId = entry.profileId,
                    visibleEntryId = entry.id,
                    orderedOwners = owners,
                )
            },
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.book")
            override val providerBindings = bindings.toList()
        }
    }

    private fun processor(): EntryDownloadProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
            every { changes } returns emptyFlow()
            every { isInitializing } returns flowOf(false)
            every { isRunning } returns flowOf(false)
            every { queueState } returns flowOf(emptyList())
            every { events } returns emptyFlow()
            every { updates() } returns emptyFlow()
            coEvery { runDownloadsUntilIdle() } returns Unit
            coEvery { deleteEntryDownloads(any()) } returns true
        }
    }
}
