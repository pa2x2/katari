package mihon.feature.graph

import kotlin.reflect.KClass

/**
 * Defines a provider-backed capability without registering it in a central catalog.
 *
 * The contract owner defines the provider API. A content type supports the capability only when its contribution
 * contains an implementation of this definition.
 */
data class CapabilityDefinition<P : Any>(
    val id: CapabilityId,
    val owner: ContributionOwner,
    val providerType: KClass<P>,
)

inline fun <reified P : Any> capabilityDefinition(
    id: CapabilityId,
    owner: ContributionOwner,
): CapabilityDefinition<P> = CapabilityDefinition(id, owner, P::class)

/** An actual capability implementation contributed by a content type. */
data class CapabilityProvider<P : Any>(
    val capability: CapabilityDefinition<P>,
    val implementation: P,
) {
    init {
        require(capability.providerType.isInstance(implementation)) {
            "Provider for ${capability.id} must implement ${capability.providerType.qualifiedName}"
        }
    }
}

/**
 * Defines media-specific work that an applicable feature cannot supply through shared behavior.
 *
 * Unlike a capability prerequisite, an absent adapter becomes an obligation only after the owning feature's
 * prerequisites are satisfied.
 */
data class SpecializedAdapterDefinition<A : Any>(
    val id: SpecializedAdapterId,
    val owner: ContributionOwner,
    val adapterType: KClass<A>,
)

inline fun <reified A : Any> specializedAdapterDefinition(
    id: SpecializedAdapterId,
    owner: ContributionOwner,
): SpecializedAdapterDefinition<A> = SpecializedAdapterDefinition(id, owner, A::class)

/** A media-specific adapter supplied by a content type for an applicable feature. */
data class SpecializedAdapter<A : Any>(
    val definition: SpecializedAdapterDefinition<A>,
    val implementation: A,
) {
    init {
        require(definition.adapterType.isInstance(implementation)) {
            "Adapter for ${definition.id} must implement ${definition.adapterType.qualifiedName}"
        }
    }
}

/** Validation-only media fixture supplied when a feature-owned behavioral contract genuinely requires one. */
data class ContractFixtureDefinition<F : Any>(
    val id: ContractFixtureId,
    val owner: ContributionOwner,
    val fixtureType: KClass<F>,
)

inline fun <reified F : Any> contractFixtureDefinition(
    id: ContractFixtureId,
    owner: ContributionOwner,
): ContractFixtureDefinition<F> = ContractFixtureDefinition(id, owner, F::class)

data class ContractFixture<F : Any>(
    val definition: ContractFixtureDefinition<F>,
    val implementation: F,
) {
    init {
        require(definition.fixtureType.isInstance(implementation)) {
            "Fixture for ${definition.id} must implement ${definition.fixtureType.qualifiedName}"
        }
    }
}

/**
 * Everything a content type currently implements.
 *
 * Any subset is valid, including no providers or a single provider during early development. Missing providers mean
 * unsupported capabilities; they are not incomplete declarations and need no explicit absence records.
 */
data class ContentTypeContribution(
    val contentType: ContentTypeId,
    val owner: ContributionOwner,
    val providers: List<CapabilityProvider<*>> = emptyList(),
    val specializedAdapters: List<SpecializedAdapter<*>> = emptyList(),
    val contractFixtures: List<ContractFixture<*>> = emptyList(),
) {
    init {
        requireUnique(
            label = "Capability providers for $contentType",
            ids = providers.map { it.capability.id.value },
        )
        requireUnique(
            label = "Specialized adapters for $contentType",
            ids = specializedAdapters.map { it.definition.id.value },
        )
        requireUnique(
            label = "Contract fixtures for $contentType",
            ids = contractFixtures.map { it.definition.id.value },
        )
    }
}

internal fun requireUnique(label: String, ids: List<String>) {
    val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
    require(duplicates.isEmpty()) {
        "$label must have unique ids; duplicates: $duplicates"
    }
}
