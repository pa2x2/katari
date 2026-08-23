package mihon.entry.interactions.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.presentation.genericEntryTypePresentation
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryStatisticsCapability
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

class EntryStatisticsFeatureTest {

    @Test
    fun `feature exposes contributed types in their declared Statistics order`() {
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                plugin(EntryType.BOOK, EntryStatisticsAccent.SAGE),
                plugin(EntryType.MANGA, EntryStatisticsAccent.ROSE),
                object : EntryInteractionPlugin {
                    override val type = EntryType.ANIME
                    override val owner = ContributionOwner("test.anime")
                    override val providerBindings =
                        emptyList<mihon.entry.interactions.runtime.EntryInteractionProviderBinding<*>>()
                },
            ),
            featureContributors = listOf(EntryStatisticsFeatureContributor),
        )
        val feature = DefaultEntryStatisticsFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.statistics,
        )

        feature.contributions shouldBe listOf(
            EntryStatisticsContribution(
                EntryType.MANGA,
                EntryStatisticsAccent.ROSE,
                MR.strings.statistics_chapters_read,
            ),
            EntryStatisticsContribution(
                EntryType.BOOK,
                EntryStatisticsAccent.SAGE,
                MR.strings.statistics_chapters_read,
            ),
        )
        feature.contribution(EntryType.ANIME) shouldBe null
    }

    private fun plugin(type: EntryType, accent: EntryStatisticsAccent): EntryInteractionPlugin {
        val provider = object : EntryStatisticsProvider {
            override val type = type
            override val accent = accent
            override val consumedUnitLabel = MR.strings.statistics_chapters_read
        }
        val presentationProvider = object : EntryTypePresentationProvider {
            override val type = type
            override val presentation = genericEntryTypePresentation
        }
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.${type.name.lowercase()}")
            override val providerBindings = listOf(
                EntryStatisticsCapability.bind(provider),
                EntryTypePresentationCapability.bind(presentationProvider),
            )
        }
    }
}
