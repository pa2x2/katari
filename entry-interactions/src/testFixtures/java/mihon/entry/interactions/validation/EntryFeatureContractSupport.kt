package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.discoverFeatureGraphContributions
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition
import mihon.feature.graph.execution.FeatureExecutionPhase
import mihon.feature.graph.featureGraphContributor

/** Evaluates [feature] against one content type that contributes no providers. */
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
    definition: FeatureExecutionParticipantDefinition<*>,
): FeatureExecutionParticipantBinding<*> {
    val typed = definition as FeatureExecutionParticipantDefinition<Any>
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
