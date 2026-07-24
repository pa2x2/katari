package mihon.entry.interactions.documentation.projection

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import mihon.entry.interactions.documentation.EntryContentTypeReferenceContextEvidenceProvider
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjectionInput
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjectionResult
import mihon.entry.interactions.documentation.EntryContentTypeReferenceRow
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSelection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.DiscoveredFeatureGraphContributions
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureProjection
import mihon.feature.graph.assembleFeatureGraph
import mihon.feature.graph.capabilityDefinition
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.featureContextRule
import mihon.feature.graph.featureProjectionDefinition
import mihon.feature.graph.featureProjectionExclusion
import org.junit.jupiter.api.Test

class EntryContentTypeReferencePlannerTest {
    private val featureOwner = ContributionOwner("future.feature")
    private val typeOwner = ContributionOwner("future.type")
    private val contextOwner = ContributionOwner("future.registration")
    private val capability = capabilityDefinition<FutureProvider>(
        CapabilityId("future.capability"),
        featureOwner,
    )
    private val context = contextInputDefinition<Boolean>(
        ContextInputId("future.registration.available"),
        contextOwner,
    )

    @Test
    fun `future content types enter selected rows without a type list`() {
        val projection = rowProjection("future.selected") { input ->
            input.requireMatchedProvider<FutureProvider>()
            EntryContentTypeReferenceStatus.SOURCE_DEPENDENT
        }
        val graph = graph(
            contentTypes = listOf(
                contentType("future.available", withProvider = true),
                contentType("future.unavailable", withProvider = false),
            ),
            feature = feature(projection),
        )

        val plan = planEntryContentTypeReference(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            contextEvidence = EntryContentTypeReferenceContextEvidenceProvider { _, _ -> null },
        )

        plan.isComplete shouldBe true
        plan.contentTypes shouldContainExactly listOf(
            ContentTypeId("future.available"),
            ContentTypeId("future.unavailable"),
        )
        plan.rows.single().statuses shouldBe mapOf(
            ContentTypeId("future.available") to EntryContentTypeReferenceStatus.SOURCE_DEPENDENT,
        )
    }

    @Test
    fun `contextual rows use authoritative evidence without becoming type support`() {
        val projection = rowProjection("future.contextual")
        val blocker = FeatureContextBlocker(
            id = FeatureArtifactId("future.registration.unavailable"),
            inputs = listOf(context),
        )
        val graph = graph(
            contentTypes = listOf(
                contentType("future.registered"),
                contentType("future.not-registered"),
            ),
            feature = feature(
                projection = projection,
                prerequisites = CapabilityExpression.Always,
                contextInputs = true,
                blocker = blocker,
            ),
        )

        val plan = planEntryContentTypeReference(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            contextEvidence = EntryContentTypeReferenceContextEvidenceProvider { subject, input ->
                if (input == context) {
                    contextEvidence(context, subject.contentType == ContentTypeId("future.registered"))
                } else {
                    null
                }
            },
        )

        plan.isComplete shouldBe true
        plan.rows.single().statuses shouldBe mapOf(
            ContentTypeId("future.registered") to EntryContentTypeReferenceStatus.SUPPORTED,
        )
    }

    @Test
    fun `conditional relationships project source dependency without invented context evidence`() {
        val projection = rowProjection(
            id = "future.source-dependent",
            selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
        ) { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT }
        val blocker = FeatureContextBlocker(
            id = FeatureArtifactId("future.registration.unavailable"),
            inputs = listOf(context),
        )
        val graph = graph(
            contentTypes = listOf(contentType("future")),
            feature = feature(
                projection = projection,
                prerequisites = CapabilityExpression.Always,
                contextInputs = true,
                blocker = blocker,
            ),
        )

        val plan = planEntryContentTypeReference(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            contextEvidence = EntryContentTypeReferenceContextEvidenceProvider { _, _ -> null },
        )

        plan.isComplete shouldBe true
        plan.rows.single().statuses shouldBe mapOf(
            ContentTypeId("future") to EntryContentTypeReferenceStatus.SOURCE_DEPENDENT,
        )
    }

    @Test
    fun `missing future evidence and participation identify owners while exclusions remain valid`() {
        val projection = rowProjection("future.contextual")
        val blocker = FeatureContextBlocker(
            id = FeatureArtifactId("future.registration.unavailable"),
            inputs = listOf(context),
        )
        val unclassifiedOwner = ContributionOwner("future.unclassified")
        val excludedOwner = ContributionOwner("future.excluded")
        val graph = graph(
            contentTypes = listOf(contentType("future")),
            feature = feature(
                projection = projection,
                prerequisites = CapabilityExpression.Always,
                contextInputs = true,
                blocker = blocker,
            ),
            additionalFeatures = listOf(
                FeatureContribution(
                    feature = FeatureId("future.excluded"),
                    owner = excludedOwner,
                    integrations = listOf(
                        FeatureIntegration(
                            id = FeatureIntegrationId("future.excluded.integration"),
                            prerequisites = CapabilityExpression.Always,
                            behaviorProjections = listOf(behavior("future.excluded.effect")),
                        ),
                    ),
                    projectionExclusions = listOf(
                        featureProjectionExclusion<EntryContentTypeReferenceProjection>("Supposedly irrelevant"),
                    ),
                ),
                FeatureContribution(
                    feature = FeatureId("future.unclassified"),
                    owner = unclassifiedOwner,
                    integrations = listOf(
                        FeatureIntegration(
                            id = FeatureIntegrationId("future.unclassified.integration"),
                            prerequisites = CapabilityExpression.Always,
                            behaviorProjections = listOf(behavior("future.unclassified.effect")),
                        ),
                    ),
                ),
            ),
        )

        val plan = planEntryContentTypeReference(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            contextEvidence = EntryContentTypeReferenceContextEvidenceProvider { _, _ -> null },
        )

        plan.isComplete shouldBe false
        plan.issues.map { it.responsibleOwner } shouldContainExactly listOf(
            unclassifiedOwner,
            contextOwner,
        )
        plan.issues.none { it.responsibleOwner == excludedOwner } shouldBe true
        plan.issues[0].details shouldContain "does not include or exclude"
        plan.issues[1].details shouldContain "Missing content-reference evidence"
    }

    private fun graph(
        contentTypes: List<ContentTypeContribution>,
        feature: FeatureContribution,
        additionalFeatures: List<FeatureContribution> = emptyList(),
    ) = assembleFeatureGraph(
        DiscoveredFeatureGraphContributions(
            contentTypes = contentTypes,
            features = listOf(feature) + additionalFeatures,
        ),
    )

    private fun contentType(
        id: String,
        withProvider: Boolean = false,
    ) = ContentTypeContribution(
        contentType = ContentTypeId(id),
        owner = typeOwner,
        providers = if (withProvider) listOf(CapabilityProvider(capability, FutureProvider())) else emptyList(),
    )

    private fun feature(
        projection: FeatureProjection<EntryContentTypeReferenceProjection>,
        prerequisites: CapabilityExpression = CapabilityExpression.Provided(capability),
        contextInputs: Boolean = false,
        blocker: FeatureContextBlocker? = null,
    ) = FeatureContribution(
        feature = FeatureId("future.feature"),
        owner = featureOwner,
        integrations = listOf(
            FeatureIntegration(
                id = FeatureIntegrationId("future.feature.integration"),
                prerequisites = prerequisites,
                contextInputs = if (contextInputs) listOf(context) else emptyList(),
                contextRule = if (contextInputs) {
                    featureContextRule(featureOwner) { evidence ->
                        if (evidence.value(context)) {
                            FeatureContextDecision.Applicable
                        } else {
                            FeatureContextDecision.Blocked(listOf(requireNotNull(blocker)))
                        }
                    }
                } else {
                    null
                },
                contextBlockers = listOfNotNull(blocker),
                projectionRequirements = listOf(projection.definition),
                projections = listOf(projection),
            ),
        ),
    )

    private fun rowProjection(
        id: String,
        selection: EntryContentTypeReferenceSelection =
            EntryContentTypeReferenceSelection.APPLICABLE_RELATIONSHIP,
        project: (EntryContentTypeReferenceProjectionInput) -> EntryContentTypeReferenceStatus = {
            EntryContentTypeReferenceStatus.SUPPORTED
        },
    ): FeatureProjection<EntryContentTypeReferenceProjection> {
        val definition = featureProjectionDefinition<EntryContentTypeReferenceProjection>(
            id = FeatureArtifactId("content-type-reference.$id"),
            owner = featureOwner,
        )
        val implementation = object : EntryContentTypeReferenceProjection {
            override val element = EntryContentTypeReferenceRow(
                id = id,
                section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
                label = "Future capability",
                order = 100,
            )
            override val selection = selection

            override fun project(input: EntryContentTypeReferenceProjectionInput) =
                EntryContentTypeReferenceProjectionResult.Cell(project(input))
        }
        return FeatureProjection(definition, implementation)
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private class FutureProvider
}
