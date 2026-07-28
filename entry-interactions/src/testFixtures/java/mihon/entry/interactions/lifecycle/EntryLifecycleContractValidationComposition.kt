package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointDefinition
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal fun lifecycleContractComposition(
    type: EntryType,
    contributor: FeatureGraphContributor,
    points: List<FeatureExecutionPointDefinition<out Any>>,
): EntryInteractionComposition {
    val plugin = object : EntryInteractionPlugin {
        override val type = type
        override val owner = ContributionOwner("lifecycle-contract.${type.name.lowercase()}")
        override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
    }
    val participants = ContractLifecycleParticipants(points)
    return createEntryInteractionComposition(
        plugins = listOf(plugin),
        featureContributors = listOf(contributor, participants),
        executionBindings = participants.definitions.map { definition -> definition.noOpBinding() },
    )
}

private class ContractLifecycleParticipants(
    points: List<FeatureExecutionPointDefinition<out Any>>,
) : FeatureGraphContributor {
    override val owner = ContributionOwner("lifecycle-contract-participants")

    private object Contract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("lifecycle-contract-participant")
    }

    val definitions = points.map { point -> point.contractParticipant() }

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        definitions.forEach(sink::add)
    }

    @Suppress("UNCHECKED_CAST")
    private fun FeatureExecutionPointDefinition<out Any>.contractParticipant() =
        FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("contract.${id.value}"),
            owner = this@ContractLifecycleParticipants.owner,
            point = this as FeatureExecutionPointDefinition<Any>,
            behavioralContracts = listOf(Contract),
        )
}

@Suppress("UNCHECKED_CAST")
private fun FeatureExecutionParticipantDefinition<out Any>.noOpBinding(): FeatureExecutionParticipantBinding<Any> {
    return FeatureExecutionParticipantBinding(
        this as FeatureExecutionParticipantDefinition<Any>,
        FeatureExecutionHandler { },
    )
}
