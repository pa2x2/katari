package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryConsumptionFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)
    private val child = EntryChapter.create().copy(id = 12L, entryId = entry.id)

    @Test
    fun `unconsuming retains the mutation result without producing a marked-consumed event`() = runTest {
        val consumedChild = child.copy(read = true)
        val processor = consumptionProcessor()
        coEvery { processor.setConsumed(entry, listOf(consumedChild), consumed = false) } returns listOf(consumedChild)
        val lifecycle = lifecycleSink()
        val feature = featureFor(plugin(EntryConsumptionCapability.bind(processor)), lifecycle)

        feature.setConsumed(entry, listOf(consumedChild), consumed = false) shouldBe
            EntryConsumptionResult.Changed(listOf(consumedChild))

        coVerify(exactly = 0) { lifecycle.onEvent(any()) }
    }

    @Test
    fun `no changed children produces no lifecycle event`() = runTest {
        val processor = consumptionProcessor()
        coEvery { processor.setConsumed(entry, listOf(child), consumed = true) } returns emptyList()
        val lifecycle = lifecycleSink()
        val feature = featureFor(plugin(EntryConsumptionCapability.bind(processor)), lifecycle)

        feature.setConsumed(entry, listOf(child), consumed = true) shouldBe EntryConsumptionResult.NoChange

        coVerify(exactly = 0) { lifecycle.onEvent(any()) }
    }

    @Test
    fun `a partial type without consumption remains valid and inapplicable`() = runTest {
        val lifecycle = lifecycleSink()
        val feature = featureFor(plugin(), lifecycle)

        feature.isApplicable(entry.type) shouldBe false
        feature.canSetConsumed(
            entry.type,
            EntryConsumptionStatus(consumed = false, hasPartialProgress = false),
            consumed = true,
        ) shouldBe false
        feature.setConsumed(entry, listOf(child), consumed = true) shouldBe
            EntryConsumptionResult.Inapplicable(entry.type)

        coVerify(exactly = 0) { lifecycle.onEvent(any()) }
    }

    private fun featureFor(
        plugin: EntryInteractionPlugin,
        lifecycle: EntryDownloadLifecycleEventSink,
    ): EntryConsumptionFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryConsumptionFeatureContributor),
        )
        return DefaultEntryConsumptionFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.consumption,
            downloadLifecycle = lifecycle,
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.partial-consumption-type")
            override val providerBindings = bindings.toList()
        }
    }

    private fun consumptionProcessor(): EntryConsumptionProcessor {
        return mockk {
            every { type } returns EntryType.BOOK
        }
    }

    private fun lifecycleSink(): EntryDownloadLifecycleEventSink {
        return mockk {
            coEvery { onEvent(any()) } returns EntryDownloadLifecycleResult.Handled
        }
    }
}
