package mihon.feature.graph.validation

import mihon.feature.graph.BehavioralContractSelection
import mihon.feature.graph.CapabilityDefinition
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.ContextInputDefinition
import mihon.feature.graph.ContractFixture
import mihon.feature.graph.ContractFixtureDefinition
import mihon.feature.graph.FeatureExecutionParticipantSubject
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.SpecializedAdapterDefinition

/** Restricted typed inputs available while a feature creates contextual validation evidence. */
class FeatureContractScenarioInput internal constructor(
    providers: List<CapabilityProvider<*>>,
    adapters: List<SpecializedAdapter<*>>,
    fixtures: List<ContractFixture<*>>,
) {
    private val inputs = FeatureContractInputs(providers, adapters, fixtures, emptyList())

    fun <P : Any> provider(definition: CapabilityDefinition<P>): P = inputs.provider(definition)

    fun <P : Any> providerOrNull(definition: CapabilityDefinition<P>): P? = inputs.providerOrNull(definition)

    fun <A : Any> adapter(definition: SpecializedAdapterDefinition<A>): A = inputs.adapter(definition)

    fun <F : Any> fixture(definition: ContractFixtureDefinition<F>): F = inputs.fixture(definition)
}

/** Exact graph-selected inputs supplied to one feature-owned verifier execution. */
class FeatureContractExecutionInput internal constructor(
    selection: BehavioralContractSelection,
) {
    val subject: FeatureIntegrationSubject = selection.subject
    private val inputs = FeatureContractInputs(
        selection.matchedProviders,
        selection.suppliedAdapters,
        selection.fixtures,
        selection.contextEvidence,
    )

    fun <P : Any> provider(definition: CapabilityDefinition<P>): P = inputs.provider(definition)

    fun <P : Any> providerOrNull(definition: CapabilityDefinition<P>): P? = inputs.providerOrNull(definition)

    fun <A : Any> adapter(definition: SpecializedAdapterDefinition<A>): A = inputs.adapter(definition)

    fun <F : Any> fixture(definition: ContractFixtureDefinition<F>): F = inputs.fixture(definition)

    fun <C : Any> evidence(definition: ContextInputDefinition<C>): C = inputs.evidence(definition)
}

/** Exact graph-selected inputs supplied to one executable-participant verifier. */
class FeatureExecutionContractExecutionInput internal constructor(
    selection: FeatureExecutionBehavioralContractSelection,
) {
    val subject: FeatureExecutionParticipantSubject = selection.subject
    private val inputs = FeatureContractInputs(
        selection.matchedProviders,
        selection.suppliedAdapters,
        selection.fixtures,
        selection.contextEvidence,
    )

    fun <P : Any> provider(definition: CapabilityDefinition<P>): P = inputs.provider(definition)

    fun <P : Any> providerOrNull(definition: CapabilityDefinition<P>): P? = inputs.providerOrNull(definition)

    fun <A : Any> adapter(definition: SpecializedAdapterDefinition<A>): A = inputs.adapter(definition)

    fun <F : Any> fixture(definition: ContractFixtureDefinition<F>): F = inputs.fixture(definition)

    fun <C : Any> evidence(definition: ContextInputDefinition<C>): C = inputs.evidence(definition)
}

internal class FeatureContractInputs(
    providers: List<CapabilityProvider<*>>,
    adapters: List<SpecializedAdapter<*>>,
    fixtures: List<ContractFixture<*>>,
    evidence: List<ContextEvidence<*>>,
) {
    private val providersById = providers.associateBy { it.capability.id }
    private val adaptersById = adapters.associateBy { it.definition.id }
    private val fixturesById = fixtures.associateBy { it.definition.id }
    private val evidenceById = evidence.associateBy { it.input.id }

    fun <P : Any> provider(definition: CapabilityDefinition<P>): P {
        return requireNotNull(providerOrNull(definition)) {
            "Contract requested unselected provider ${definition.id}"
        }
    }

    fun <P : Any> providerOrNull(definition: CapabilityDefinition<P>): P? {
        val selected = providersById[definition.id] ?: return null
        require(selected.capability == definition) {
            "Contract requested contradictory provider definition ${definition.id}"
        }
        @Suppress("UNCHECKED_CAST")
        return selected.implementation as P
    }

    fun <A : Any> adapter(definition: SpecializedAdapterDefinition<A>): A {
        val selected = requireNotNull(adaptersById[definition.id]) {
            "Contract requested unselected adapter ${definition.id}"
        }
        require(selected.definition == definition) {
            "Contract requested contradictory adapter definition ${definition.id}"
        }
        @Suppress("UNCHECKED_CAST")
        return selected.implementation as A
    }

    fun <F : Any> fixture(definition: ContractFixtureDefinition<F>): F {
        val selected = requireNotNull(fixturesById[definition.id]) {
            "Contract requested unselected fixture ${definition.id}"
        }
        require(selected.definition == definition) {
            "Contract requested contradictory fixture definition ${definition.id}"
        }
        @Suppress("UNCHECKED_CAST")
        return selected.implementation as F
    }

    fun <C : Any> evidence(definition: ContextInputDefinition<C>): C {
        val selected = requireNotNull(evidenceById[definition.id]) {
            "Contract requested unselected context evidence ${definition.id}"
        }
        require(selected.input == definition) {
            "Contract requested contradictory context definition ${definition.id}"
        }
        @Suppress("UNCHECKED_CAST")
        return selected.value as C
    }
}
