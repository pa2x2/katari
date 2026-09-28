package mihon.feature.graph

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FeatureSubjectScopeTest {

    private val applicationOwner = ContributionOwner("example.application")
    private val featureOwner = ContributionOwner("example.feature")

    @Test
    fun `integrations are evaluated only against subjects in their declared scope`() {
        val adapter = specializedAdapterDefinition<ApplicationAdapter>(
            id = SpecializedAdapterId("example.application-adapter"),
            owner = featureOwner,
        )
        val contentTypes = listOf(contentType("book"), contentType("manga"))
        val graph = graph(
            application = ApplicationSubjectContribution(applicationOwner),
            contentTypes = contentTypes,
            integrations = listOf(
                integration(
                    id = "example.application",
                    scope = FeatureSubjectScope.Application,
                    specializedRequirements = listOf(adapter),
                ),
                integration("example.entry", FeatureSubjectScope.EntryContentType),
            ),
        )
        val evaluation = evaluateFeatureGraph(graph)
        val subjects = evaluation.integrations.map { integration ->
            integration.subject.affectedSubject.id to integration.subject.integration
        }

        subjects shouldContainExactly buildList {
            add(FeatureSubjectId.Application to FeatureIntegrationId("example.application"))
            contentTypes.forEach { contentType ->
                add(contentType.subject to FeatureIntegrationId("example.entry"))
            }
        }
        evaluation.obligations.single().responsibleOwner shouldBe applicationOwner
    }

    private fun graph(
        application: ApplicationSubjectContribution,
        contentTypes: List<ContentTypeContribution>,
        integrations: List<FeatureIntegration>,
    ): FeatureGraph {
        return assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = contentTypes,
                features = listOf(
                    FeatureContribution(
                        feature = FeatureId("example"),
                        owner = featureOwner,
                        integrations = integrations,
                    ),
                ),
                applicationSubjects = listOf(application),
            ),
        )
    }

    private fun contentType(id: String) = ContentTypeContribution(
        contentType = ContentTypeId(id),
        owner = ContributionOwner("example.$id"),
    )

    private fun integration(
        id: String,
        scope: FeatureSubjectScope,
        specializedRequirements: List<SpecializedAdapterDefinition<*>> = emptyList(),
    ) = FeatureIntegration(
        id = FeatureIntegrationId(id),
        prerequisites = CapabilityExpression.Always,
        subjectScope = scope,
        specializedRequirements = specializedRequirements,
        behaviorProjections = listOf(behavior("$id.behavior")),
    )

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private class ApplicationAdapter
}
