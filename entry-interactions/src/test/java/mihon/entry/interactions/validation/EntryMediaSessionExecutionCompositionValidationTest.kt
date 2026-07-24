package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.DefaultEntryMediaSessionFeature
import mihon.entry.interactions.ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT
import mihon.entry.interactions.EntryHistoryFeature
import mihon.entry.interactions.EntryHistoryFeatureContributor
import mihon.entry.interactions.EntryInteractionComposition
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryMediaSessionCapability
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionFeatureContributor
import mihon.entry.interactions.EntryMediaSessionIncognitoContributor
import mihon.entry.interactions.EntryMediaSessionIncognitoState
import mihon.entry.interactions.EntryMediaSessionProcessor
import mihon.entry.interactions.EntryMediaSessionResult
import mihon.entry.interactions.createEntryInteractionComposition
import mihon.entry.interactions.entryHistoryMediaSessionBinding
import mihon.entry.interactions.entryMediaSessionIncognitoBinding
import mihon.entry.interactions.mediaSessionContractEvent
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.repository.EntryRepository

class EntryMediaSessionExecutionCompositionValidationTest {
    @Test
    fun `unknown future consequence joins without coordinator changes`() = runTest {
        val type = EntryType.entries.first()
        val observed = mutableListOf<EntryMediaSessionEvent>()
        val futureBinding = FeatureExecutionParticipantBinding(
            definition = FUTURE_PARTICIPANT,
            handler = FeatureExecutionHandler { execution -> observed += execution.event },
        )
        val composition = composition(
            entryType = type,
            contributors = listOf(
                EntryMediaSessionFeatureContributor,
                EntryMediaSessionIncognitoContributor,
                FutureConsequenceContributor,
            ),
            bindings = listOf(
                entryMediaSessionIncognitoBinding(
                    repository = { mockk(relaxed = true) },
                    incognitoState = EntryMediaSessionIncognitoState { false },
                ),
                futureBinding,
            ),
        )
        val event = mediaSessionContractEvent(type)

        DefaultEntryMediaSessionFeature(
            evaluation = composition.featureGraphEvaluation,
            executions = composition.featureExecutions,
        ).onEvent(event)

        observed.shouldContainExactly(event)
    }

    @Test
    fun `incognito blocks recording consequences without blocking unrelated participants`() = runTest {
        val type = EntryType.entries.first()
        val event = mediaSessionContractEvent(type)
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(event.child.entryId) } returns event.visibleEntry
        }
        val history = mockk<EntryHistoryFeature>(relaxed = true)
        val unrelated = mutableListOf<EntryMediaSessionEvent>()
        val composition = composition(
            entryType = type,
            contributors = listOf(
                EntryMediaSessionFeatureContributor,
                EntryMediaSessionIncognitoContributor,
                EntryHistoryFeatureContributor,
                FutureConsequenceContributor,
            ),
            bindings = listOf(
                entryMediaSessionIncognitoBinding(
                    repository = { repository },
                    incognitoState = EntryMediaSessionIncognitoState { true },
                ),
                entryHistoryMediaSessionBinding { history },
                FeatureExecutionParticipantBinding(
                    definition = FUTURE_PARTICIPANT,
                    handler = FeatureExecutionHandler { execution -> unrelated += execution.event },
                ),
            ),
        )

        DefaultEntryMediaSessionFeature(
            evaluation = composition.featureGraphEvaluation,
            executions = composition.featureExecutions,
        ).onEvent(event)

        coVerify(exactly = 0) { history.record(any(), any()) }
        unrelated.shouldContainExactly(event)
    }

    private fun composition(
        entryType: EntryType,
        contributors: List<FeatureGraphContributor>,
        bindings: List<FeatureExecutionParticipantBinding<*>>,
    ): EntryInteractionComposition {
        val processor = object : EntryMediaSessionProcessor {
            override val type = entryType

            override suspend fun onEvent(event: EntryMediaSessionEvent) = EntryMediaSessionResult.Handled
        }
        val plugin = object : EntryInteractionPlugin {
            override val type = entryType
            override val owner = ContributionOwner("test.media-session-type")
            override val providerBindings = listOf(EntryMediaSessionCapability.bind(processor))
        }
        return createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = contributors,
            executionBindings = bindings,
        )
    }

    private companion object {
        val FUTURE_OWNER = ContributionOwner("test.future-media-consequence")

        object FutureContract : FeatureBehaviorContract {
            override val id = FeatureArtifactId("test.future-media-consequence.behavior")
        }

        val FUTURE_PARTICIPANT = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.future-media-consequence"),
            owner = FUTURE_OWNER,
            point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
            prerequisites = CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
            behavioralContracts = listOf(FutureContract),
        )

        val FutureConsequenceContributor = featureGraphContributor(FUTURE_OWNER) {
            add(FUTURE_PARTICIPANT)
        }
    }
}
