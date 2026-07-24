package mihon.feature.graph

import kotlin.reflect.KClass

/** Owner-defined metadata carried with a context input for projections and validation. */
interface ContextInputMetadata

/** Context whose value is supplied later without flattening it into type-wide capability support. */
data class ContextInputDefinition<C : Any>(
    val id: ContextInputId,
    val owner: ContributionOwner,
    val valueType: KClass<C>,
    val metadata: Set<ContextInputMetadata> = emptySet(),
)

inline fun <reified C : Any> contextInputDefinition(
    id: ContextInputId,
    owner: ContributionOwner,
    metadata: Set<ContextInputMetadata> = emptySet(),
): ContextInputDefinition<C> = ContextInputDefinition(id, owner, C::class, metadata)

/** Feature-owned interpretation of a complete contextual evidence snapshot. */
interface FeatureContextRule {
    val owner: ContributionOwner

    fun evaluate(evidence: FeatureContextEvidence): FeatureContextDecision
}

fun featureContextRule(
    owner: ContributionOwner,
    evaluate: (FeatureContextEvidence) -> FeatureContextDecision,
): FeatureContextRule {
    return object : FeatureContextRule {
        override val owner = owner

        override fun evaluate(evidence: FeatureContextEvidence): FeatureContextDecision = evaluate(evidence)
    }
}

/** Result of interpreting complete contextual evidence for one candidate integration. */
sealed interface FeatureContextDecision {
    data object Applicable : FeatureContextDecision

    data class Blocked(
        val blockers: List<FeatureContextBlocker>,
    ) : FeatureContextDecision {
        init {
            require(blockers.isNotEmpty()) { "A blocked context decision requires at least one blocker" }
            requireUnique("Context blockers", blockers.map { it.id.value })
        }
    }
}

/** Feature-owned reason why supplied context may prevent one candidate integration from applying. */
data class FeatureContextBlocker(
    val id: FeatureArtifactId,
    val inputs: List<ContextInputDefinition<*>>,
) {
    init {
        require(inputs.isNotEmpty()) { "Context blocker $id must identify at least one context input" }
        requireUnique("Context blocker $id inputs", inputs.map { it.id.value })
    }
}
