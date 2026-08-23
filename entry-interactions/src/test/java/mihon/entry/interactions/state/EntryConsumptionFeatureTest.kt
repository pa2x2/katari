package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.download.EntryDownloadLifecycleResult
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
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
        val history = mockk<EntryHistoryFeature>(relaxed = true)
        val feature = featureFor(plugin(EntryConsumptionCapability.bind(processor)), lifecycle, history)

        feature.setConsumed(entry, listOf(consumedChild), consumed = false) shouldBe
            EntryConsumptionResult.Changed(listOf(consumedChild))

        coVerify(exactly = 0) { lifecycle.onEvent(any()) }
        coVerify(exactly = 0) { history.recordManualCompletions(any(), any()) }
    }

    @Test
    fun `no changed children produces no lifecycle event`() = runTest {
        val processor = consumptionProcessor()
        coEvery { processor.setConsumed(entry, listOf(child), consumed = true) } returns emptyList()
        val lifecycle = lifecycleSink()
        val history = mockk<EntryHistoryFeature>(relaxed = true)
        val feature = featureFor(plugin(EntryConsumptionCapability.bind(processor)), lifecycle, history)

        feature.setConsumed(entry, listOf(child), consumed = true) shouldBe EntryConsumptionResult.NoChange

        coVerify(exactly = 0) { lifecycle.onEvent(any()) }
        coVerify(exactly = 0) { history.recordManualCompletions(any(), any()) }
    }

    @Test
    fun `marking children consumed records manual completions once for changed children`() = runTest {
        val consumedChild = child.copy(read = true)
        val processor = consumptionProcessor()
        coEvery { processor.setConsumed(entry, listOf(child), consumed = true) } returns listOf(consumedChild)
        val lifecycle = lifecycleSink()
        val history = mockk<EntryHistoryFeature>(relaxed = true)
        val feature = featureFor(plugin(EntryConsumptionCapability.bind(processor)), lifecycle, history)

        feature.setConsumed(entry, listOf(child), consumed = true) shouldBe
            EntryConsumptionResult.Changed(listOf(consumedChild))

        coVerify(exactly = 1) { history.recordManualCompletions(entry, listOf(consumedChild)) }
        coVerify(exactly = 1) { lifecycle.onEvent(any()) }
    }

    @Test
    fun `a partial type without consumption remains valid and inapplicable`() = runTest {
        val lifecycle = lifecycleSink()
        val history = mockk<EntryHistoryFeature>(relaxed = true)
        val feature = featureFor(plugin(), lifecycle, history)

        feature.isApplicable(entry.type) shouldBe false
        feature.canSetConsumed(
            entry.type,
            EntryConsumptionStatus(consumed = false, hasPartialProgress = false),
            consumed = true,
        ) shouldBe false
        feature.setConsumed(entry, listOf(child), consumed = true) shouldBe
            EntryConsumptionResult.Inapplicable(entry.type)

        coVerify(exactly = 0) { lifecycle.onEvent(any()) }
        coVerify(exactly = 0) { history.recordManualCompletions(any(), any()) }
    }

    private fun featureFor(
        plugin: EntryInteractionPlugin,
        lifecycle: EntryDownloadLifecycleEventSink,
        history: EntryHistoryFeature,
    ): EntryConsumptionFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryConsumptionFeatureContributor),
        )
        return DefaultEntryConsumptionFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.consumption,
            downloadLifecycle = lifecycle,
            history = history,
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
