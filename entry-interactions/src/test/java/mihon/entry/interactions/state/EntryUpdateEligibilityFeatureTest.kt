package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryUpdateEligibilityFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `release-window restriction uses contextual fetch bounds`() {
        val feature = featureFor(compositionFor(EntryType.BOOK), policy())
        val outsideWindow = entry.copy(nextUpdate = 200L)

        feature.evaluate(
            request(outsideWindow, fetchWindowUpperBound = 100L),
        ) shouldBe EntryUpdateEligibility.Skipped(EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD)
        feature.evaluate(
            request(outsideWindow, fetchWindowUpperBound = null),
        ) shouldBe EntryUpdateEligibility.Eligible
    }

    @Test
    fun `unknown progress evidence does not trigger progress-dependent skips`() {
        val feature = featureFor(compositionFor(EntryType.BOOK), policy())

        feature.evaluate(
            request(
                entry.copy(updateStrategy = EntryUpdateStrategy.ONLY_FETCH_ONCE),
                totalCount = null,
                unconsumedCount = null,
                hasStarted = null,
            ),
        ) shouldBe EntryUpdateEligibility.Eligible
    }

    private fun compositionFor(type: EntryType): EntryInteractionComposition {
        val plugin = object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        }
        return createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryUpdateEligibilityFeatureContributor),
        )
    }

    private fun featureFor(
        composition: EntryInteractionComposition,
        policy: EntryUpdateEligibilityPolicy,
    ): EntryUpdateEligibilityFeature {
        return DefaultEntryUpdateEligibilityFeature(
            evaluation = composition.featureGraphEvaluation,
            currentPolicy = { policy },
        )
    }

    private fun request(
        entry: Entry,
        totalCount: Long? = 0L,
        unconsumedCount: Long? = 0L,
        hasStarted: Boolean? = true,
        fetchWindowUpperBound: Long? = null,
    ): EntryUpdateEligibilityRequest {
        return EntryUpdateEligibilityRequest(
            entry = entry,
            totalCount = totalCount,
            unconsumedCount = unconsumedCount,
            hasStarted = hasStarted,
            fetchWindowUpperBound = fetchWindowUpperBound,
        )
    }

    private fun policy(
        skipCompleted: Boolean = true,
        skipWhenUnconsumed: Boolean = true,
        skipWhenNotStarted: Boolean = true,
        skipOutsideReleasePeriod: Boolean = true,
    ): EntryUpdateEligibilityPolicy {
        return EntryUpdateEligibilityPolicy(
            skipCompleted = skipCompleted,
            skipWhenUnconsumed = skipWhenUnconsumed,
            skipWhenNotStarted = skipWhenNotStarted,
            skipOutsideReleasePeriod = skipOutsideReleasePeriod,
        )
    }
}
