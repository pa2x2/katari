package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryDownloadMaintenanceFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `a core Download provider activates every maintenance behavior`() = runTest {
        val processor = processor()
        val oldSource = mockk<UnifiedSource>()
        val newSource = mockk<UnifiedSource>()
        every { processor.hasDownloads(entry) } returnsMany listOf(true, true, false)
        val feature = featureFor(EntryDownloadCapability.bind(processor))

        feature.invalidateCaches() shouldBe
            EntryDownloadMaintenanceResult.Performed
        feature.renameSource(oldSource, newSource) shouldBe EntryDownloadMaintenanceResult.Performed
        feature.renameEntry(entry, "Renamed") shouldBe EntryDownloadMaintenanceResult.Performed
        feature.inspectEntry(entry) shouldBe EntryDownloadMaintenanceInspection.HasDownloads
        feature.removeEntryDownloads(entry) shouldBe
            EntryDownloadMaintenanceResult.Performed

        verify(exactly = 1) { processor.invalidateCache() }
        verify(exactly = 1) { processor.renameSource(oldSource, newSource) }
        coVerify(exactly = 1) { processor.renameEntry(entry, "Renamed") }
        verify(exactly = 3) { processor.hasDownloads(entry) }
        coVerify(exactly = 1) { processor.deleteEntryDownloads(entry) }
    }

    @Test
    fun `provider absence is valid and distinct from an entry with no downloads`() = runTest {
        val ownership = mockk<EntryMergeDownloadOwnershipProjection>(relaxed = true)
        val featureWithoutProvider = featureFor(ownership = ownership)

        featureWithoutProvider.inspectEntry(entry) shouldBe
            EntryDownloadMaintenanceInspection.Inapplicable(EntryType.BOOK)
        featureWithoutProvider.removeEntryDownloads(entry) shouldBe
            EntryDownloadMaintenanceResult.Inapplicable(setOf(EntryType.BOOK))
        featureWithoutProvider.invalidateCaches() shouldBe EntryDownloadMaintenanceResult.NoParticipants
        featureWithoutProvider.renameSource(mockk(), mockk()) shouldBe EntryDownloadMaintenanceResult.NoParticipants
        coVerify(exactly = 0) { ownership.resolveDownloadOwners(any()) }

        val processor = processor()
        every { processor.hasDownloads(entry) } returns false
        val featureWithEmptyStorage = featureFor(EntryDownloadCapability.bind(processor))

        featureWithEmptyStorage.inspectEntry(entry) shouldBe EntryDownloadMaintenanceInspection.NoDownloads
    }

    @Test
    fun `merged download maintenance visits each concrete owner`() = runTest {
        val member = entry.copy(id = 8L, url = "/member")
        val processor = processor()
        every { processor.hasDownloads(entry) } returnsMany listOf(false, false)
        every { processor.hasDownloads(member) } returnsMany listOf(true, true, false)
        val feature = featureFor(
            EntryDownloadCapability.bind(processor),
            owners = listOf(entry, member),
        )

        feature.inspectEntry(entry) shouldBe EntryDownloadMaintenanceInspection.HasDownloads
        feature.removeEntryDownloads(entry) shouldBe EntryDownloadMaintenanceResult.Performed

        verify(exactly = 2) { processor.hasDownloads(entry) }
        verify(exactly = 3) { processor.hasDownloads(member) }
        coVerify(exactly = 0) { processor.deleteEntryDownloads(entry) }
        coVerify(exactly = 1) { processor.deleteEntryDownloads(member) }
    }

    @Test
    fun `removal remains incomplete while a captured owner still reports downloads`() = runTest {
        val processor = processor()
        every { processor.hasDownloads(entry) } returns true
        val feature = featureFor(EntryDownloadCapability.bind(processor))

        val preparation = feature.prepareRemoval(entry)
            as EntryDownloadRemovalPreparation.Prepared

        feature.applyRemoval(preparation.plan) shouldBe EntryDownloadMaintenanceResult.Incomplete(listOf(entry))
    }

    @Test
    fun `provider deletion failure cannot be acknowledged from an evicted cache`() = runTest {
        val processor = processor()
        every { processor.hasDownloads(entry) } returns true
        coEvery { processor.deleteEntryDownloads(entry) } returns false
        val feature = featureFor(EntryDownloadCapability.bind(processor))
        val plan = (feature.prepareRemoval(entry) as EntryDownloadRemovalPreparation.Prepared).plan

        feature.applyRemoval(plan) shouldBe EntryDownloadMaintenanceResult.Incomplete(listOf(entry))

        verify(exactly = 1) { processor.hasDownloads(entry) }
    }

    private fun featureFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
        owners: List<Entry> = listOf(entry),
        ownership: EntryMergeDownloadOwnershipProjection? = null,
    ): EntryDownloadMaintenanceFeature {
        val plugins = bindings
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(plugin(*it)) }
            .orEmpty()
        val composition = createEntryInteractionComposition(
            plugins = plugins,
            featureContributors = listOf(EntryDownloadMaintenanceFeatureContributor),
        )
        return DefaultEntryDownloadMaintenanceFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.download,
            ownership = ownership ?: mockk {
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
            every { queueStatusUpdates() } returns emptyFlow()
            every { queueProgressUpdates() } returns emptyFlow()
            coEvery { runDownloadsUntilIdle() } returns Unit
            coEvery { deleteEntryDownloads(any()) } returns true
        }
    }
}
