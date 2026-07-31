package mihon.feature.graph.validation

import mihon.feature.graph.ContractFixtureDefinition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.execution.FeatureExecutionParticipantSubject

data class MissingFeatureExecutionContractVerifierObligation(
    override val responsibleOwner: ContributionOwner,
    val contract: FeatureExecutionContractReference,
    val affectedSubjects: List<FeatureExecutionParticipantSubject>,
) : FeatureContractValidationObligation

data class MissingFeatureExecutionContractScenarioObligation(
    override val responsibleOwner: ContributionOwner,
    val contract: FeatureExecutionContractReference,
    val affectedSubjects: List<FeatureExecutionParticipantSubject>,
) : FeatureContractValidationObligation

data class InvalidFeatureExecutionContractScenarioObligation(
    override val responsibleOwner: ContributionOwner,
    val subject: FeatureExecutionParticipantSubject,
    val scenario: OwnedFeatureExecutionContractScenario,
    val reason: String,
) : FeatureContractValidationObligation

data class MissingFeatureExecutionContractFixtureObligation(
    override val responsibleOwner: ContributionOwner,
    val subject: FeatureExecutionParticipantSubject,
    val requirement: ContractFixtureDefinition<*>,
    val affectedContracts: List<FeatureBehaviorContract>,
) : FeatureContractValidationObligation
