package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class FeatureSubjectScopeTest {

    private val applicationOwner = ContributionOwner("example.application")
    private val featureOwner = ContributionOwner("example.feature")
    private val capability = capabilityDefinition<ApplicationCapability>(
        CapabilityId("example.application-capability"),
        applicationOwner,
    )

    @Test
    fun `integrations are evaluated only against subjects in their declared scope`() {
        val adapter = specializedAdapterDefinition<ApplicationAdapter>(
            id = SpecializedAdapterId("example.application-adapter"),
            owner = featureOwner,
        )
        val contentTypeSets = listOf(
            emptyList(),
            listOf(contentType("book")),
            listOf(contentType("book"), contentType("manga")),
        )

        contentTypeSets.forEach { contentTypes ->
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
    }

    @Test
    fun `application context and artifacts use application-owned evidence and fixtures`() {
        val input = contextInputDefinition<Boolean>(
            id = ContextInputId("example.enabled"),
            owner = featureOwner,
        )
        val fixtureDefinition = contractFixtureDefinition<String>(
            id = ContractFixtureId("example.fixture"),
            owner = featureOwner,
        )
        val fixture = ContractFixture(fixtureDefinition, "application-fixture")
        val contract = object : FeatureBehaviorContract {
            override val id = FeatureArtifactId("example.contract")
            override val fixtureRequirements = listOf(fixtureDefinition)
        }
        val integration = FeatureIntegration(
            id = FeatureIntegrationId("example.contextual"),
            prerequisites = CapabilityExpression.Provided(capability),
            subjectScope = FeatureSubjectScope.Application,
            contextInputs = listOf(input),
            contextRule = featureContextRule(featureOwner) { FeatureContextDecision.Applicable },
            behavioralContracts = listOf(contract),
        )
        val graph = graph(
            application = ApplicationSubjectContribution(
                owner = applicationOwner,
                providers = listOf(CapabilityProvider(capability, ApplicationCapability())),
                contractFixtures = listOf(fixture),
            ),
            contentTypes = listOf(contentType("book")),
            integrations = listOf(integration),
        )
        val evaluation = evaluateFeatureGraph(graph)

        shouldThrow<IllegalStateException> {
            resolveFeatureContext(
                evaluation = evaluation,
                subject = FeatureSubjectId.EntryContentType(ContentTypeId("book")),
                feature = FeatureId("example"),
                integration = integration.id,
                evidence = listOf(contextEvidence(input, true)),
            )
        }.message shouldContain "found 0"

        val resolved = resolveFeatureContext(
            evaluation = evaluation,
            subject = FeatureSubjectId.Application,
            feature = FeatureId("example"),
            integration = integration.id,
            evidence = listOf(contextEvidence(input, true)),
        )
        val selected = selectContextualFeatureArtifacts(graph, evaluation, resolved)

        (resolved.integration as ApplicableFeatureContext).subject.affectedSubject shouldBe
            FeatureSubjectReference(FeatureSubjectId.Application, applicationOwner)
        selected.behavioralContracts.single().fixtures shouldContainExactly listOf(fixture)
        selected.obligations shouldBe emptyList()
    }

    @Test
    fun `subject providers are reachable only from integrations in the same scope`() {
        val error = shouldThrow<IllegalStateException> {
            graph(
                application = ApplicationSubjectContribution(
                    owner = applicationOwner,
                    providers = listOf(CapabilityProvider(capability, ApplicationCapability())),
                ),
                contentTypes = listOf(contentType("book")),
                integrations = listOf(
                    FeatureIntegration(
                        id = FeatureIntegrationId("example.entry"),
                        prerequisites = CapabilityExpression.Provided(capability),
                        subjectScope = FeatureSubjectScope.EntryContentType,
                        behaviorProjections = listOf(behavior("example.entry-behavior")),
                    ),
                ),
            )
        }

        error.message shouldContain
            "Unreachable capability provider example.application-capability on application"
    }

    @Test
    fun `duplicate application subjects are rejected deterministically`() {
        val otherOwner = ContributionOwner("example.other-application")

        shouldThrow<IllegalStateException> {
            assembleFeatureGraph(
                DiscoveredFeatureGraphContributions(
                    contentTypes = emptyList(),
                    features = emptyList(),
                    applicationSubjects = listOf(
                        ApplicationSubjectContribution(applicationOwner),
                        ApplicationSubjectContribution(otherOwner),
                    ),
                ),
            )
        }.message shouldContain
            "Duplicate application subject contribution from owners [example.application, example.other-application]"
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

    private class ApplicationCapability

    private class ApplicationAdapter
}
