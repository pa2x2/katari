package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryInteractionProviderBinding
import mihon.entry.interactions.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionPhase
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.discoverFeatureGraphContributions
import mihon.feature.graph.featureGraphContributor
import mihon.feature.graph.validation.FeatureContractFailure
import mihon.feature.graph.validation.FeatureContractVerificationResult

/**
 * Builds the smallest owner graph needed to execute a shared Feature coordinator for an already selected production
 * provider. The verifier cannot name or enroll a content type; production graph selection supplied the binding first.
 */
internal fun productionSubjectEvaluation(
    binding: EntryInteractionProviderBinding<*>,
    feature: FeatureGraphContributor,
): FeatureGraphEvaluation = productionSubjectEvaluation(listOf(binding), feature)

/** Multi-provider equivalent for a selected integration whose prerequisite expression joins capabilities. */
internal fun productionSubjectEvaluation(
    bindings: List<EntryInteractionProviderBinding<*>>,
    feature: FeatureGraphContributor,
): FeatureGraphEvaluation {
    require(bindings.isNotEmpty()) { "A provider-backed contract subject needs at least one binding" }
    val type = bindings.first().implementation.type
    require(bindings.all { it.implementation.type == type }) {
        "A contract subject cannot combine providers from different entry types"
    }
    val plugin = object : EntryInteractionPlugin {
        override val type = type
        override val owner = ContributionOwner("contract-subject.${type.name.lowercase()}")
        override val providerBindings = bindings
    }
    val contributors = validationContributors(feature)
    val executionBindings = validationExecutionBindings(contributors)
    return createEntryInteractionComposition(
        plugins = listOf(plugin),
        featureContributors = contributors,
        executionBindings = executionBindings.immediate,
        durableExecutionBindings = executionBindings.durable,
    ).featureGraphEvaluation
}

/** Provider-free equivalent for an unconditional Feature and an already selected production subject. */
internal fun productionSubjectEvaluation(
    type: EntryType,
    feature: FeatureGraphContributor,
    specializedAdapters: List<SpecializedAdapter<*>> = emptyList(),
    additionalContributors: List<FeatureGraphContributor> = emptyList(),
): FeatureGraphEvaluation {
    val plugin = object : EntryInteractionPlugin {
        override val type = type
        override val owner = ContributionOwner("contract-subject.${type.name.lowercase()}")
        override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        override val specializedAdapters = specializedAdapters
    }
    val contributors = validationContributors(feature, additionalContributors)
    val bindings = validationExecutionBindings(contributors)
    return createEntryInteractionComposition(
        plugins = listOf(plugin),
        featureContributors = contributors,
        executionBindings = bindings.immediate,
        durableExecutionBindings = bindings.durable,
    ).featureGraphEvaluation
}

private fun validationExecutionBindings(
    contributors: List<FeatureGraphContributor>,
): ValidationExecutionBindings {
    val participants = discoverFeatureGraphContributions(contributors).executionParticipants
    return ValidationExecutionBindings(
        immediate = participants
            .filter { it.point.phase != FeatureExecutionPhase.Durable }
            .map(::noOpBinding),
        durable = participants
            .filter { it.point.phase == FeatureExecutionPhase.Durable }
            .map(::noOpDurableBinding),
    )
}

private fun validationContributors(
    feature: FeatureGraphContributor,
    additionalContributors: List<FeatureGraphContributor> = emptyList(),
): List<FeatureGraphContributor> {
    val contributors = listOf(feature) + additionalContributors
    val discovered = discoverFeatureGraphContributions(contributors)
    val declaredPointIds = discovered.executionPoints.mapTo(mutableSetOf()) { it.id }
    val pointOwners = discovered.executionParticipants
        .map { it.point }
        .filterNot { it.id in declaredPointIds }
        .distinctBy { it.id }
        .map { point -> featureGraphContributor(point.owner) { add(point) } }
    return contributors + pointOwners
}

@Suppress("UNCHECKED_CAST")
private fun noOpBinding(
    definition: mihon.feature.graph.FeatureExecutionParticipantDefinition<*>,
): FeatureExecutionParticipantBinding<*> {
    val typed = definition as mihon.feature.graph.FeatureExecutionParticipantDefinition<Any>
    return FeatureExecutionParticipantBinding(typed, FeatureExecutionHandler { })
}

@Suppress("UNCHECKED_CAST")
private fun noOpDurableBinding(
    definition: FeatureExecutionParticipantDefinition<*>,
): FeatureDurableExecutionParticipantBinding<*> {
    val typed = definition as FeatureExecutionParticipantDefinition<Any>
    return FeatureDurableExecutionParticipantBinding(
        definition = typed,
        preparer = { null },
        deliveryHandler = { },
    )
}

private data class ValidationExecutionBindings(
    val immediate: List<FeatureExecutionParticipantBinding<*>>,
    val durable: List<FeatureDurableExecutionParticipantBinding<*>>,
)

internal suspend fun verifyFeatureContract(block: suspend () -> Unit): FeatureContractVerificationResult {
    return try {
        block()
        FeatureContractVerificationResult.Passed
    } catch (mismatch: FeatureContractMismatch) {
        FeatureContractVerificationResult.Failed(listOf(FeatureContractFailure(mismatch.message.orEmpty())))
    }
}

internal fun contractExpectation(condition: Boolean, message: String) {
    if (!condition) throw FeatureContractMismatch(message)
}

private class FeatureContractMismatch(message: String) : IllegalStateException(message)
