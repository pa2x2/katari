package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryContinueFeatureTest {
    private val context = mockk<Context>(relaxed = true)
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `an applicable provider reports that no next child exists`() = runTest {
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryContinueCapability.bind(RecordingContinueProcessor(EntryType.BOOK, null))),
        )

        feature.continueEntry(context, entry) shouldBe EntryContinueResult.NoNext
    }

    @Test
    fun `missing Continue provider is valid and exposes no Continue action`() = runTest {
        val feature = featureFor()

        feature.isApplicable(entry.type) shouldBe false
        feature.nextTarget(entry) shouldBe EntryContinueTargetResult.Inapplicable
        feature.continueEntry(context, entry) shouldBe EntryContinueResult.Inapplicable
    }

    private fun featureFor(vararg plugins: EntryInteractionPlugin): EntryContinueFeature {
        val composition = createEntryInteractionComposition(
            plugins = plugins.toList(),
            featureContributors = listOf(EntryContinueFeatureContributor),
        )
        return DefaultEntryContinueFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.continueEntry,
        )
    }

    private fun plugin(
        type: EntryType,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = bindings.toList()
        }
    }

    private class RecordingContinueProcessor(
        override val type: EntryType,
        private val next: EntryChapter?,
    ) : EntryContinueProcessor {
        override suspend fun findNext(entry: Entry): EntryChapter? = next

        override fun open(context: Context, entry: Entry, chapter: EntryChapter) = Unit
    }
}
