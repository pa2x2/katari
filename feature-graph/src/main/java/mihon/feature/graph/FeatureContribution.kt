package mihon.feature.graph

import kotlin.reflect.KClass

/** A positive capability expression. Missing prerequisites make a feature inapplicable and create no obligation. */
sealed interface CapabilityExpression {
    data object Always : CapabilityExpression

    data class Provided(
        val capability: CapabilityDefinition<*>,
    ) : CapabilityExpression

    data class AllOf(
        val terms: List<CapabilityExpression>,
    ) : CapabilityExpression {
        init {
            requireTerms("AllOf", terms)
        }
    }

    data class AnyOf(
        val terms: List<CapabilityExpression>,
    ) : CapabilityExpression {
        init {
            requireTerms("AnyOf", terms)
        }
    }
}

fun allOf(vararg terms: CapabilityExpression): CapabilityExpression = CapabilityExpression.AllOf(terms.toList())

fun anyOf(vararg terms: CapabilityExpression): CapabilityExpression = CapabilityExpression.AnyOf(terms.toList())

private fun requireTerms(label: String, terms: List<CapabilityExpression>) {
    require(terms.isNotEmpty()) {
        "$label requires at least one term; use Always for an unconditional feature"
    }
    require(terms.distinct().size == terms.size) {
        "$label cannot contain duplicate terms"
    }
}

/**
 * Describes observable behavior supplied by the owning Feature when an integration applies.
 *
 * This projection is reportable metadata for behavior the owning Feature coordinator already guarantees. Its id is not
 * a runtime dispatch key or implementation binding. Independently contributed executable work must be declared through
 * a [FeatureExecutionParticipantDefinition].
 */
interface FeatureBehaviorProjection {
    val id: FeatureArtifactId
}

/** Behavioral expectations selected automatically when an integration applies. */
interface FeatureBehaviorContract {
    val id: FeatureArtifactId

    /** Empty for shared contracts that can execute directly from matched providers and adapters. */
    val fixtureRequirements: List<ContractFixtureDefinition<*>>
        get() = emptyList()
}

/** Feature-owned requirement for one executable developer or user-facing projection. */
data class FeatureProjectionDefinition<P : Any>(
    val id: FeatureArtifactId,
    val owner: ContributionOwner,
    val projectionType: KClass<P>,
)

inline fun <reified P : Any> featureProjectionDefinition(
    id: FeatureArtifactId,
    owner: ContributionOwner,
): FeatureProjectionDefinition<P> = FeatureProjectionDefinition(id, owner, P::class)

/** Executable projection implementation supplied against a feature-owned requirement. */
data class FeatureProjection<P : Any>(
    val definition: FeatureProjectionDefinition<P>,
    val implementation: P,
) {
    init {
        require(definition.projectionType.isInstance(implementation)) {
            "Projection for ${definition.id} must implement ${definition.projectionType.qualifiedName}"
        }
    }
}

/** Explicit feature-level exclusion from one optional projection channel. */
data class FeatureProjectionExclusion(
    val projectionType: KClass<*>,
    val reason: String,
) {
    init {
        require(reason.isNotBlank()) {
            "Projection exclusion for ${projectionType.qualifiedName} requires a reason"
        }
    }
}

inline fun <reified P : Any> featureProjectionExclusion(reason: String): FeatureProjectionExclusion =
    FeatureProjectionExclusion(P::class, reason)

/**
 * One feature-owned relationship between provider-backed prerequisites and their behavior projections.
 *
 * Context inputs describe additional runtime information that later evaluation must retain. Specialized prerequisites
 * make the relationship inapplicable when the affected type has not contributed that media-specific participation.
 * Specialized requirements instead become obligations after the other prerequisites establish applicability.
 */
data class FeatureIntegration(
    val id: FeatureIntegrationId,
    val prerequisites: CapabilityExpression,
    val contextInputs: List<ContextInputDefinition<*>> = emptyList(),
    val contextRule: FeatureContextRule? = null,
    val contextBlockers: List<FeatureContextBlocker> = emptyList(),
    val specializedPrerequisites: List<SpecializedAdapterDefinition<*>> = emptyList(),
    val specializedRequirements: List<SpecializedAdapterDefinition<*>> = emptyList(),
    val behaviorProjections: List<FeatureBehaviorProjection> = emptyList(),
    val behavioralContracts: List<FeatureBehaviorContract> = emptyList(),
    val projectionRequirements: List<FeatureProjectionDefinition<*>> = emptyList(),
    val projections: List<FeatureProjection<*>> = emptyList(),
) {
    init {
        requireUnique("Context inputs for $id", contextInputs.map { it.id.value })
        require(contextInputs.isNotEmpty() == (contextRule != null)) {
            "Feature integration $id must declare context inputs and a context rule together"
        }
        requireUnique("Context blockers for $id", contextBlockers.map { it.id.value })
        val declaredContextInputs = contextInputs.toSet()
        contextBlockers.forEach { blocker ->
            blocker.inputs.forEach { input ->
                require(input in declaredContextInputs) {
                    "Context blocker ${blocker.id} on $id references undeclared input ${input.id}"
                }
            }
        }
        requireUnique("Specialized prerequisites for $id", specializedPrerequisites.map { it.id.value })
        requireUnique("Specialized requirements for $id", specializedRequirements.map { it.id.value })
        require(
            specializedPrerequisites.none { prerequisite ->
                specializedRequirements.any { requirement -> requirement.id == prerequisite.id }
            },
        ) {
            "Feature integration $id cannot use one specialized adapter as both a prerequisite and a requirement"
        }
        requireUnique("Behavior projections for $id", behaviorProjections.map { it.id.value })
        requireUnique("Behavioral contracts for $id", behavioralContracts.map { it.id.value })
        behavioralContracts.forEach { contract ->
            requireUnique(
                "Fixture requirements for ${contract.id}",
                contract.fixtureRequirements.map { it.id.value },
            )
        }
        requireUnique("Projection requirements for $id", projectionRequirements.map { it.id.value })
        requireUnique("Projections for $id", projections.map { it.definition.id.value })
        val requirementIds = projectionRequirements.mapTo(mutableSetOf()) { it.id }
        projections.forEach { projection ->
            require(projection.definition.id in requirementIds) {
                "Projection ${projection.definition.id} on $id has no matching requirement"
            }
        }
    }
}

/**
 * Feature-owned integration knowledge.
 *
 * Content types never enumerate these integrations. Nested behavior projections, contracts, and projections inherit
 * this
 * contribution's owner; specialized adapter contracts must be defined by the same owner.
 */
data class FeatureContribution(
    val feature: FeatureId,
    val owner: ContributionOwner,
    val integrations: List<FeatureIntegration>,
    val projectionExclusions: List<FeatureProjectionExclusion> = emptyList(),
) {
    init {
        requireUnique("Integrations for $feature", integrations.map { it.id.value })
        requireUnique(
            "Projection exclusions for $feature",
            projectionExclusions.map { it.projectionType.qualifiedName ?: it.projectionType.toString() },
        )
        integrations.mapNotNull { it.contextRule }.forEach { rule ->
            require(rule.owner == owner) {
                "Feature $feature cannot use context rule owned by ${rule.owner}"
            }
        }
        integrations.flatMap { it.specializedPrerequisites + it.specializedRequirements }.forEach { requirement ->
            require(requirement.owner == owner) {
                "Feature $feature cannot use specialized adapter ${requirement.id} declared by ${requirement.owner}"
            }
        }
        integrations
            .flatMap { it.behavioralContracts }
            .flatMap { it.fixtureRequirements }
            .forEach { requirement ->
                require(requirement.owner == owner) {
                    "Feature $feature cannot own contract fixture ${requirement.id} declared by ${requirement.owner}"
                }
            }
        integrations.flatMap { it.projectionRequirements }.forEach { requirement ->
            require(requirement.owner == owner) {
                "Feature $feature cannot own projection requirement ${requirement.id} declared by ${requirement.owner}"
            }
        }
        integrations.flatMap { it.projections }.forEach { projection ->
            require(projection.definition.owner == owner) {
                "Feature $feature cannot supply projection ${projection.definition.id} declared by " +
                    projection.definition.owner
            }
        }
        val requiredProjectionTypes = integrations
            .flatMap { it.projectionRequirements }
            .mapTo(mutableSetOf()) { it.projectionType }
        projectionExclusions.forEach { exclusion ->
            require(exclusion.projectionType !in requiredProjectionTypes) {
                "Feature $feature cannot require and exclude projection channel " +
                    (exclusion.projectionType.qualifiedName ?: exclusion.projectionType.toString())
            }
        }
    }
}
