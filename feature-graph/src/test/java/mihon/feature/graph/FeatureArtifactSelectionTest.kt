package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class FeatureArtifactSelectionTest {

    private val contractOwner = ContributionOwner("example.contract")
    private val featureOwner = ContributionOwner("example.feature")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)

    @Test
    fun `applicable integration may require no contract fixture or projection`() {
        val graph = graph(
            contentTypes = listOf(contentType(id = "subject")),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Always,
                behaviorProjections = listOf(behavior("example.projection")),
            ),
        )

        val selected = selectFeatureArtifacts(graph, evaluateFeatureGraph(graph))

        selected.behavioralContracts shouldBe emptyList()
        selected.projections shouldBe emptyList()
        selected.obligations shouldBe emptyList()
    }

    @Test
    fun `applicable contributions select and execute the same feature owned artifacts`() {
        val adapterDefinition = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val contract = RecordingContract("example.behavior")
        val projectionDefinition = featureProjectionDefinition<RecordingProjection>(
            id = FeatureArtifactId("example.reference"),
            owner = featureOwner,
        )
        val projectionImplementation = RecordingProjection()
        val projection = FeatureProjection(projectionDefinition, projectionImplementation)
        val graph = graph(
            contentTypes = listOf(
                contentType(
                    id = "complete",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                    adapters = listOf(SpecializedAdapter(adapterDefinition, ExampleAdapter())),
                ),
                contentType(
                    id = "second",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                    adapters = listOf(SpecializedAdapter(adapterDefinition, ExampleAdapter())),
                ),
                contentType(
                    id = "incomplete",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
                contentType(id = "unsupported"),
            ),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Provided(alpha),
                specializedRequirements = listOf(adapterDefinition),
                behavioralContracts = listOf(contract),
                projectionRequirements = listOf(projectionDefinition),
                projections = listOf(projection),
            ),
        )

        val selected = selectFeatureArtifacts(graph, evaluateFeatureGraph(graph))

        selected.behavioralContracts.map { it.subject.contentType.value } shouldContainExactly
            listOf("complete", "second")
        selected.projections.map { it.subject.contentType.value } shouldContainExactly listOf("complete", "second")
        selected.behavioralContracts.all { it.contract === contract } shouldBe true
        selected.projections.all { it.projection === projection } shouldBe true
        selected.projections.all { it.projection.implementation === projectionImplementation } shouldBe true
        selected.projections.all { selection ->
            selection.matchedProviders.single().capability == alpha &&
                selection.suppliedAdapters.single().definition == adapterDefinition &&
                selection.contextEvidence.isEmpty()
        } shouldBe true
        selected.obligations shouldBe emptyList()

        selected.behavioralContracts.forEach { (it.contract as RecordingContract).execute(it.subject) }
        selected.projections.forEach {
            (it.projection.implementation as RecordingProjection).project(it.subject)
        }
        contract.executedSubjects shouldContainExactly listOf(ContentTypeId("complete"), ContentTypeId("second"))
        projectionImplementation.projectedSubjects shouldContainExactly listOf(
            ContentTypeId("complete"),
            ContentTypeId("second"),
        )
    }

    @Test
    fun `missing contract fixture is attributed only to the affected content type owner`() {
        val fixtureDefinition = contractFixtureDefinition<ExampleFixture>(
            id = ContractFixtureId("example.fixture"),
            owner = featureOwner,
        )
        val contract = RecordingContract(
            id = "example.behavior",
            fixtureRequirements = listOf(fixtureDefinition),
        )
        val secondContract = RecordingContract(
            id = "example.second-behavior",
            fixtureRequirements = listOf(fixtureDefinition),
        )
        val suppliedFixture = ContractFixture(fixtureDefinition, ExampleFixture("ready"))
        val graph = graph(
            contentTypes = listOf(
                contentType(
                    id = "missing",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
                contentType(
                    id = "supplied",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                    fixtures = listOf(suppliedFixture),
                ),
            ),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Provided(alpha),
                behavioralContracts = listOf(secondContract, contract),
            ),
        )

        val selected = selectFeatureArtifacts(graph, evaluateFeatureGraph(graph))

        selected.behavioralContracts shouldHaveSize 4
        selected.behavioralContracts.filter { it.subject.contentType.value == "missing" }
            .all { it.fixtures.isEmpty() } shouldBe true
        selected.behavioralContracts.filter { it.subject.contentType.value == "supplied" }
            .all { it.fixtures == listOf(suppliedFixture) } shouldBe true
        val obligation = selected.obligations.single() as MissingContractFixtureObligation
        obligation.responsibleOwner shouldBe ContributionOwner("missing.type")
        obligation.subject.contentType shouldBe ContentTypeId("missing")
        obligation.requirement shouldBe fixtureDefinition
        obligation.affectedContracts shouldContainExactly listOf(contract, secondContract)
    }

    @Test
    fun `one missing shared projection obligation names every affected subject`() {
        val contract = RecordingContract("example.behavior")
        val projectionDefinition = featureProjectionDefinition<RecordingProjection>(
            id = FeatureArtifactId("example.reference"),
            owner = featureOwner,
        )
        val graph = graph(
            contentTypes = listOf(
                contentType(
                    id = "zeta",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
                contentType(
                    id = "alpha",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
            ),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Provided(alpha),
                behavioralContracts = listOf(contract),
                projectionRequirements = listOf(projectionDefinition),
            ),
        )

        val selected = selectFeatureArtifacts(graph, evaluateFeatureGraph(graph))

        selected.projections shouldBe emptyList()
        val obligation = selected.obligations.single() as MissingFeatureProjectionObligation
        obligation.responsibleOwner shouldBe featureOwner
        obligation.requirement shouldBe projectionDefinition
        obligation.affectedSubjects.map { it.contentType.value } shouldContainExactly listOf("alpha", "zeta")
    }

    @Test
    fun `artifact ordering is deterministic across declared order`() {
        val alphaContract = RecordingContract("example.alpha-contract")
        val zetaContract = RecordingContract("example.zeta-contract")
        val alphaProjection = projection("example.alpha-projection")
        val zetaProjection = projection("example.zeta-projection")
        val graph = graph(
            contentTypes = listOf(
                contentType(
                    id = "subject",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
            ),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Provided(alpha),
                behavioralContracts = listOf(zetaContract, alphaContract),
                projectionRequirements = listOf(zetaProjection.definition, alphaProjection.definition),
                projections = listOf(zetaProjection, alphaProjection),
            ),
        )

        val selected = selectFeatureArtifacts(graph, evaluateFeatureGraph(graph))

        selected.behavioralContracts.map { it.contract.id.value } shouldContainExactly listOf(
            "example.alpha-contract",
            "example.zeta-contract",
        )
        selected.projections.map { it.projection.definition.id.value } shouldContainExactly listOf(
            "example.alpha-projection",
            "example.zeta-projection",
        )
    }

    @Test
    fun `selection rejects a curated subset of evaluated relationships`() {
        val contract = RecordingContract("example.behavior")
        val graph = graph(
            contentTypes = listOf(
                contentType(
                    id = "subject",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
            ),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Provided(alpha),
                behavioralContracts = listOf(contract),
            ),
        )
        val incompleteEvaluation = evaluateFeatureGraph(graph).copy(integrations = emptyList())

        val failure = shouldThrow<IllegalStateException> {
            selectFeatureArtifacts(graph, incompleteEvaluation)
        }

        failure.message shouldContain "evaluation coverage mismatch"
    }

    private fun graph(
        contentTypes: List<ContentTypeContribution>,
        integration: FeatureIntegration,
    ): FeatureGraph {
        return assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = contentTypes,
                features = listOf(
                    FeatureContribution(
                        feature = FeatureId("example"),
                        owner = featureOwner,
                        integrations = listOf(integration),
                    ),
                ),
            ),
        )
    }

    private fun contentType(
        id: String,
        providers: List<CapabilityProvider<*>> = emptyList(),
        adapters: List<SpecializedAdapter<*>> = emptyList(),
        fixtures: List<ContractFixture<*>> = emptyList(),
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = ContributionOwner("$id.type"),
            providers = providers,
            specializedAdapters = adapters,
            contractFixtures = fixtures,
        )
    }

    private fun projection(id: String): FeatureProjection<RecordingProjection> {
        val definition = featureProjectionDefinition<RecordingProjection>(FeatureArtifactId(id), featureOwner)
        return FeatureProjection(definition, RecordingProjection())
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private class RecordingContract(
        id: String,
        override val fixtureRequirements: List<ContractFixtureDefinition<*>> = emptyList(),
    ) : FeatureBehaviorContract {
        override val id = FeatureArtifactId(id)
        val executedSubjects = mutableListOf<ContentTypeId>()

        fun execute(subject: FeatureIntegrationSubject) {
            executedSubjects += subject.contentType
        }
    }

    private class RecordingProjection {
        val projectedSubjects = mutableListOf<ContentTypeId>()

        fun project(subject: FeatureIntegrationSubject) {
            projectedSubjects += subject.contentType
        }
    }

    private class AlphaProvider

    private class ExampleAdapter

    private data class ExampleFixture(val state: String)
}
