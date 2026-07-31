package mihon.feature.graph

import mihon.feature.graph.execution.FeatureExecutionParticipantSubject

data class SpecializedExecutionParticipantObligation(
    override val responsibleOwner: ContributionOwner,
    val subject: FeatureExecutionParticipantSubject,
    val requirement: SpecializedAdapterDefinition<*>,
) : FeatureObligation
