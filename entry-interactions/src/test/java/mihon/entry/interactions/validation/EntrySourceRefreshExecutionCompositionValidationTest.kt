package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.entry.interactions.source.DefaultEntrySourceRefreshFeature
import mihon.entry.interactions.source.ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT
import mihon.entry.interactions.source.EntrySourceRefreshFeatureContributor
import mihon.entry.interactions.source.EntrySourceRefreshNewChildrenEvent
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class EntrySourceRefreshExecutionCompositionValidationTest {
    @Test
    fun `only a manual refresh hands inserted children to new-children consequences`() = runTest {
        val type = EntryType.entries.first()
        val entry = Entry.create().copy(id = 91L, source = 92L, type = type)
        val child = EntryChapter.create().copy(id = 93L, entryId = entry.id)
        val handedOff = mutableListOf<EntrySourceRefreshNewChildrenEvent>()
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = type
                    override val owner = ContributionOwner("test.source-refresh-type")
                    override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
                },
            ),
            featureContributors = listOf(
                EntrySourceRefreshFeatureContributor,
                featureGraphContributor(CONSEQUENCE_OWNER) { add(CONSEQUENCE) },
            ),
            executionBindings = listOf(
                FeatureExecutionParticipantBinding(CONSEQUENCE, FeatureExecutionHandler { handedOff += it }),
            ),
        )
        val feature = DefaultEntrySourceRefreshFeature(
            evaluation = composition.featureGraphEvaluation,
            executions = composition.featureExecutions,
            sourceManager = mockk<SourceManager> {
                every { get(entry.source) } returns mockk<UnifiedSource>()
            },
            syncEntryWithSource = mockk<SyncEntryWithSource> {
                coEvery { syncStrictly(any(), any(), any(), any(), any(), any(), any()) } returns
                    SyncEntryWithSource.SyncResult(listOf(child), 1, 0, 0, false)
            },
            updateLibraryTitles = { false },
        )

        feature.refresh(EntrySourceRefreshRequest(entry, manual = false))
        feature.refresh(EntrySourceRefreshRequest(entry, manual = true))

        handedOff.map { it.entry to it.newChildren }.shouldContainExactly(entry to listOf(child))
    }

    private companion object {
        val CONSEQUENCE_OWNER = ContributionOwner("test.source-refresh-consequence")

        object ConsequenceContract : FeatureBehaviorContract {
            override val id = FeatureArtifactId("test.source-refresh-consequence.behavior")
        }

        val CONSEQUENCE = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.source-refresh-consequence"),
            owner = CONSEQUENCE_OWNER,
            point = ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT,
            prerequisites = CapabilityExpression.Always,
            behavioralContracts = listOf(ConsequenceContract),
        )
    }
}
