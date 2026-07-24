package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.DefaultEntryLibraryUpdateRefreshFeature
import mihon.entry.interactions.DefaultEntrySourceRefreshFeature
import mihon.entry.interactions.ENTRY_LIBRARY_UPDATE_NEW_CHILDREN_EXECUTION_POINT
import mihon.entry.interactions.ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryInteractionProviderBinding
import mihon.entry.interactions.EntryLibraryUpdateNewChildrenEvent
import mihon.entry.interactions.EntryLibraryUpdateRefreshFeatureContributor
import mihon.entry.interactions.EntryLibraryUpdateRefreshRequest
import mihon.entry.interactions.EntrySourceRefreshFeatureContributor
import mihon.entry.interactions.EntrySourceRefreshNewChildrenEvent
import mihon.entry.interactions.EntrySourceRefreshRequest
import mihon.entry.interactions.EntrySourceRefreshResult
import mihon.entry.interactions.createEntryInteractionComposition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class EntryRefreshExecutionCompositionValidationTest {
    @Test
    fun `unknown future consequences join refresh lifecycles without coordinator changes`() = runTest {
        val type = EntryType.entries.first()
        val entry = Entry.create().copy(id = 91L, source = 92L, type = type)
        val child = EntryChapter.create().copy(id = 93L, entryId = entry.id)
        val sourceEvents = mutableListOf<EntrySourceRefreshNewChildrenEvent>()
        val libraryEvents = mutableListOf<EntryLibraryUpdateNewChildrenEvent>()
        var libraryCompletionCount = 0
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(type)),
            featureContributors = listOf(
                EntrySourceRefreshFeatureContributor,
                EntryLibraryUpdateRefreshFeatureContributor,
                FutureRefreshConsequenceContributor,
            ),
            executionBindings = listOf(
                FeatureExecutionParticipantBinding(
                    definition = FUTURE_SOURCE_PARTICIPANT,
                    handler = FeatureExecutionHandler { event -> sourceEvents += event },
                ),
                FeatureExecutionParticipantBinding(
                    definition = FUTURE_LIBRARY_PARTICIPANT,
                    handler = FeatureExecutionHandler { event ->
                        libraryEvents += event
                        event.session.state(
                            participant = FUTURE_LIBRARY_PARTICIPANT.id,
                            create = { Any() },
                            complete = { libraryCompletionCount++ },
                        )
                    },
                ),
            ),
        )
        val sourceRefresh = DefaultEntrySourceRefreshFeature(
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

        sourceRefresh.refresh(EntrySourceRefreshRequest(entry, manual = true)) shouldBe
            EntrySourceRefreshResult.Refreshed(listOf(child), 1, 0, 0, false)
        sourceRefresh.refresh(EntrySourceRefreshRequest(entry, manual = false)) shouldBe
            EntrySourceRefreshResult.Refreshed(listOf(child), 1, 0, 0, false)
        sourceEvents.map { it.entry to it.newChildren }.shouldContainExactly(entry to listOf(child))

        val libraryRefresh = DefaultEntryLibraryUpdateRefreshFeature(
            evaluation = composition.featureGraphEvaluation,
            sourceRefresh = mockk {
                coEvery { refresh(any()) } returns
                    EntrySourceRefreshResult.Refreshed(listOf(child), 1, 0, 0, false)
            },
            executions = composition.featureExecutions,
        )
        val session = libraryRefresh.newSession()
        session.refresh(EntryLibraryUpdateRefreshRequest(entry, true, 0L, 0L))

        libraryEvents.map { it.entry to it.newChildren }.shouldContainExactly(entry to listOf(child))
        libraryCompletionCount shouldBe 0
        session.complete()
        session.complete()
        libraryCompletionCount shouldBe 1
    }

    private fun plugin(type: EntryType): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.refresh-type")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        }
    }

    private companion object {
        val FUTURE_OWNER = ContributionOwner("test.future-refresh-consequence")

        object FutureSourceRefreshContract : FeatureBehaviorContract {
            override val id = FeatureArtifactId("test.future-source-refresh-consequence.behavior")
        }

        object FutureLibraryRefreshContract : FeatureBehaviorContract {
            override val id = FeatureArtifactId("test.future-library-refresh-consequence.behavior")
        }

        val FUTURE_SOURCE_PARTICIPANT = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.future-source-refresh-consequence"),
            owner = FUTURE_OWNER,
            point = ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT,
            prerequisites = CapabilityExpression.Always,
            behavioralContracts = listOf(FutureSourceRefreshContract),
        )

        val FUTURE_LIBRARY_PARTICIPANT = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.future-library-refresh-consequence"),
            owner = FUTURE_OWNER,
            point = ENTRY_LIBRARY_UPDATE_NEW_CHILDREN_EXECUTION_POINT,
            prerequisites = CapabilityExpression.Always,
            behavioralContracts = listOf(FutureLibraryRefreshContract),
        )

        val FutureRefreshConsequenceContributor = featureGraphContributor(FUTURE_OWNER) {
            add(FUTURE_SOURCE_PARTICIPANT)
            add(FUTURE_LIBRARY_PARTICIPANT)
        }
    }
}
