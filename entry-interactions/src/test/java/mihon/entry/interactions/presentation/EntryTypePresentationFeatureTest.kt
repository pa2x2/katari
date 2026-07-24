package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test

class EntryTypePresentationFeatureTest {

    @Test
    fun `provider absence stays valid and resolves as observable generic vocabulary`() {
        val feature = feature(
            object : EntryInteractionPlugin {
                override val type = EntryType.BOOK
                override val owner = ContributionOwner("test.partial")
                override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
            },
        )

        val concrete = feature.presentation(EntryType.BOOK)
            .shouldBeInstanceOf<EntryTypePresentationResult.Generic>()
        val mixed = feature.presentation(null)
            .shouldBeInstanceOf<EntryTypePresentationResult.Generic>()

        concrete.type shouldBe EntryType.BOOK
        concrete.presentation shouldBe genericEntryTypePresentation
        mixed.type shouldBe null
    }

    private fun feature(plugin: EntryInteractionPlugin): EntryTypePresentationFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryTypePresentationFeatureContributor),
        )
        return DefaultEntryTypePresentationFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.typePresentation,
        )
    }
}
