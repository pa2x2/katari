package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class FeatureGraphAssemblyTest {

    private val contractOwner = ContributionOwner("example.contract")
    private val featureOwner = ContributionOwner("example.feature")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)

    @Test
    fun `discovered contributions enter the graph without assembler knowledge`() {
        val typesOwner = ContributionOwner("example.types")
        val ownedTypes = mutableListOf(type("first", typesOwner))
        val typeContributor = featureGraphContributor(typesOwner) {
            ownedTypes.forEach(::add)
        }
        val featureContributor = featureContributor(alpha)

        discoverAndAssembleFeatureGraph(listOf(typeContributor, featureContributor)).contentTypes
            .map { it.contentType } shouldContainExactly listOf(ContentTypeId("first"))

        ownedTypes += type("second", typesOwner)

        discoverAndAssembleFeatureGraph(listOf(typeContributor, featureContributor)).contentTypes
            .map { it.contentType } shouldContainExactly listOf(ContentTypeId("first"), ContentTypeId("second"))
    }

    @Test
    fun `new provider and feature contributions enter an existing discovery pipeline`() {
        val typesOwner = ContributionOwner("example.types")
        val providers = mutableListOf<CapabilityProvider<*>>(CapabilityProvider(alpha, AlphaProvider()))
        val features = mutableListOf(feature(alpha, "alpha-feature"))
        val typeContributor = featureGraphContributor(typesOwner) {
            add(
                ContentTypeContribution(
                    contentType = ContentTypeId("example"),
                    owner = typesOwner,
                    providers = providers.toList(),
                ),
            )
        }
        val featureContributor = featureGraphContributor(featureOwner) {
            features.forEach(::add)
        }
        val contributors = listOf(typeContributor, featureContributor)

        discoverAndAssembleFeatureGraph(contributors).capabilities
            .map { it.id } shouldContainExactly listOf(alpha.id)

        val beta = capabilityDefinition<BetaProvider>(CapabilityId("example.beta"), contractOwner)
        providers += CapabilityProvider(beta, BetaProvider())
        features += feature(beta, "beta-feature")

        val expanded = discoverAndAssembleFeatureGraph(contributors)
        expanded.capabilities.map { it.id } shouldContainExactly listOf(alpha.id, beta.id)
        expanded.features.map { it.feature.value } shouldContainExactly listOf("alpha-feature", "beta-feature")
    }

    @Test
    fun `graph ordering is deterministic across contributor order`() {
        val typesOwner = ContributionOwner("example.types")
        val types = featureGraphContributor(typesOwner) {
            add(type("zeta", typesOwner))
            add(type("alpha", typesOwner))
        }
        val feature = featureContributor(alpha)

        val forward = discoverAndAssembleFeatureGraph(listOf(types, feature))
        val reverse = discoverAndAssembleFeatureGraph(listOf(feature, types))

        forward shouldContainSameGraphAs reverse
        forward.contentTypes.map { it.contentType.value } shouldContainExactly listOf("alpha", "zeta")
    }

    @Test
    fun `duplicate content type and feature contributions are rejected`() {
        val duplicateType = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(
                    typeContributor(type("example", ContributionOwner("owner.first"))),
                    typeContributor(type("example", ContributionOwner("owner.second"))),
                    featureContributor(alpha),
                ),
            )
        }
        duplicateType.message shouldContain "Duplicate content-type contribution example"

        val duplicateFeature = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(
                    typeContributor(type("example")),
                    featureGraphContributor(featureOwner) {
                        add(feature(alpha))
                        add(feature(alpha))
                    },
                ),
            )
        }
        duplicateFeature.message shouldContain "Duplicate feature contribution example-feature"
    }

    @Test
    fun `contributor cannot submit another owner's top-level contribution`() {
        val failure = shouldThrow<IllegalArgumentException> {
            discoverFeatureGraphContributions(
                listOf(
                    featureGraphContributor(ContributionOwner("owner.first")) {
                        add(type("example", ContributionOwner("owner.second")))
                    },
                ),
            )
        }

        failure.message shouldContain "Contributor owner.first cannot submit content type example owned by owner.second"
    }

    @Test
    fun `contradictory distributed capability definitions are rejected`() {
        val contradictory = capabilityDefinition<OtherAlphaProvider>(
            id = alpha.id,
            owner = ContributionOwner("other.contract"),
        )

        val failure = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(
                    typeContributor(type("example")),
                    featureContributor(contradictory),
                ),
            )
        }

        failure.message shouldContain "Contradictory capability definition example.alpha"
    }

    @Test
    fun `provider without a consuming feature relationship is rejected`() {
        val failure = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(typeContributor(type("example"))),
            )
        }

        failure.message shouldContain "Unreachable capability provider example.alpha on example"
    }

    @Test
    fun `feature may prepare for a provider that no content type implements yet`() {
        val graph = discoverAndAssembleFeatureGraph(
            listOf(featureContributor(alpha)),
        )

        graph.contentTypes shouldContainExactly emptyList()
        graph.capabilities.map { it.id } shouldContainExactly listOf(alpha.id)
    }

    @Test
    fun `unused specialized adapter and effectless integration are rejected`() {
        val adapterDefinition = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val unusedAdapter = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(
                    typeContributor(
                        ContentTypeContribution(
                            contentType = ContentTypeId("example"),
                            owner = ContributionOwner("example.type"),
                            providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                            specializedAdapters = listOf(
                                SpecializedAdapter(adapterDefinition, ExampleAdapter()),
                            ),
                        ),
                    ),
                    featureContributor(alpha),
                ),
            )
        }
        unusedAdapter.message shouldContain "Unreachable specialized adapter example.adapter on example"

        val effectless = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(
                    featureGraphContributor(featureOwner) {
                        add(
                            FeatureContribution(
                                feature = FeatureId("effectless"),
                                owner = featureOwner,
                                integrations = listOf(
                                    FeatureIntegration(
                                        id = FeatureIntegrationId("effectless.integration"),
                                        prerequisites = CapabilityExpression.Always,
                                    ),
                                ),
                            ),
                        )
                    },
                ),
            )
        }
        effectless.message shouldContain "Unreachable feature integration effectless.integration"
    }

    @Test
    fun `contract fixture without a requiring behavioral contract is rejected`() {
        val fixtureDefinition = contractFixtureDefinition<ExampleFixture>(
            id = ContractFixtureId("example.fixture"),
            owner = featureOwner,
        )
        val fixture = ContractFixture(fixtureDefinition, ExampleFixture())

        val failure = shouldThrow<IllegalStateException> {
            discoverAndAssembleFeatureGraph(
                listOf(
                    typeContributor(
                        ContentTypeContribution(
                            contentType = ContentTypeId("example"),
                            owner = ContributionOwner("example.type"),
                            providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                            contractFixtures = listOf(fixture),
                        ),
                    ),
                    featureContributor(alpha),
                ),
            )
        }

        failure.message shouldContain "Unreachable contract fixture example.fixture on example"
    }

    private fun type(
        id: String,
        owner: ContributionOwner = ContributionOwner("$id.type"),
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = owner,
            providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
        )
    }

    private fun typeContributor(type: ContentTypeContribution): FeatureGraphContributor {
        return featureGraphContributor(type.owner) { add(type) }
    }

    private fun featureContributor(capability: CapabilityDefinition<*>): FeatureGraphContributor {
        return featureGraphContributor(featureOwner) { add(feature(capability)) }
    }

    private fun feature(
        capability: CapabilityDefinition<*>,
        id: String = "example-feature",
    ): FeatureContribution {
        return FeatureContribution(
            feature = FeatureId(id),
            owner = featureOwner,
            integrations = listOf(
                FeatureIntegration(
                    id = FeatureIntegrationId("$id.integration"),
                    prerequisites = CapabilityExpression.Provided(capability),
                    behaviorProjections = listOf(behavior("$id.projection")),
                ),
            ),
        )
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private infix fun FeatureGraph.shouldContainSameGraphAs(other: FeatureGraph) {
        contentTypes.map { it.contentType } shouldContainExactly other.contentTypes.map { it.contentType }
        features.map { it.feature } shouldContainExactly other.features.map { it.feature }
        executionPoints.map { it.id } shouldContainExactly other.executionPoints.map { it.id }
        executionParticipants.map { it.id } shouldContainExactly other.executionParticipants.map { it.id }
        capabilities.map { it.id } shouldContainExactly other.capabilities.map { it.id }
        contractFixtures.map { it.id } shouldContainExactly other.contractFixtures.map { it.id }
        projections.map { it.id } shouldContainExactly other.projections.map { it.id }
    }

    private class AlphaProvider

    private class OtherAlphaProvider

    private class BetaProvider

    private class ExampleAdapter

    private class ExampleFixture
}
