package mihon.feature.graph.validation

import mihon.feature.graph.ApplicableFeatureExecutionContext
import mihon.feature.graph.ApplicableFeatureExecutionParticipant
import mihon.feature.graph.BlockedFeatureExecutionContext
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ConditionalFeatureExecutionParticipant
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.ContractFixture
import mihon.feature.graph.ContractFixtureDefinition
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantSubject
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureObligation
import mihon.feature.graph.IncompleteFeatureExecutionContext
import mihon.feature.graph.MissingFeatureExecutionContextEvidence
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.resolveFeatureExecutionContext

data class FeatureExecutionBehavioralContractSelection(
    val subject: FeatureExecutionParticipantSubject,
    val contract: FeatureBehaviorContract,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val fixtures: List<ContractFixture<*>>,
    val contextEvidence: List<ContextEvidence<*>>,
)

data class FeatureExecutionContractExecutionSelection(
    val contractSelection: FeatureExecutionBehavioralContractSelection,
    val scenario: OwnedFeatureExecutionContractScenario?,
    val verifier: OwnedFeatureExecutionContractVerifier,
)

data class MissingFeatureExecutionContractVerifierObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val contract: FeatureExecutionContractReference,
    val affectedSubjects: List<FeatureExecutionParticipantSubject>,
) : FeatureContractValidationObligation

data class MissingFeatureExecutionContractScenarioObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val contract: FeatureExecutionContractReference,
    val affectedSubjects: List<FeatureExecutionParticipantSubject>,
) : FeatureContractValidationObligation

data class InvalidFeatureExecutionContractScenarioObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val subject: FeatureExecutionParticipantSubject,
    val scenario: OwnedFeatureExecutionContractScenario,
    val reason: String,
) : FeatureContractValidationObligation

data class MissingFeatureExecutionContractFixtureObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val subject: FeatureExecutionParticipantSubject,
    val requirement: ContractFixtureDefinition<*>,
    val affectedContracts: List<FeatureBehaviorContract>,
) : FeatureContractValidationObligation

internal data class FeatureExecutionContractValidationPlanPart(
    val executions: List<FeatureExecutionContractExecutionSelection>,
    val graphObligations: List<FeatureObligation>,
    val obligations: List<FeatureContractValidationObligation>,
)

internal fun planFeatureExecutionContractValidation(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    contributions: DiscoveredFeatureValidationContributions,
): FeatureExecutionContractValidationPlanPart {
    val declarations = graph.executionParticipants.flatMap { participant ->
        participant.behavioralContracts.map { contract ->
            FeatureExecutionContractReference(participant.id, contract) to DeclaredExecutionContract(
                participant.owner,
                contract,
            )
        }
    }.associate { it }
    validateExecutionContractContributions(graph, declarations, contributions)

    val verifiersByContract = contributions.executionVerifiers.associateBy { it.verifier.contract }
    val scenariosByContract = contributions.executionScenarios.groupBy { it.scenario.contract }
    val obligations = mutableListOf<FeatureContractValidationObligation>()
    val graphObligations = mutableListOf<FeatureObligation>()
    val executions = mutableListOf<FeatureExecutionContractExecutionSelection>()

    evaluation.executionParticipants.forEach { evaluated ->
        when (evaluated) {
            is ApplicableFeatureExecutionParticipant -> {
                evaluated.participant.behavioralContracts.forEach { contract ->
                    selectExecutionContract(
                        graph = graph,
                        subject = evaluated.subject,
                        contract = contract,
                        matchedProviders = evaluated.matchedProviders,
                        suppliedAdapters = evaluated.suppliedAdapters,
                        evidence = emptyList(),
                        verifier = verifiersByContract[contract.reference(evaluated.subject)],
                        scenario = null,
                        obligations = obligations,
                        executions = executions,
                    )
                }
            }
            is ConditionalFeatureExecutionParticipant -> {
                evaluated.participant.behavioralContracts.forEach { contract ->
                    val reference = contract.reference(evaluated.subject)
                    val verifier = verifiersByContract[reference]
                    if (verifier == null) {
                        obligations += MissingFeatureExecutionContractVerifierObligation(
                            responsibleOwner = evaluated.subject.participantOwner,
                            contract = reference,
                            affectedSubjects = listOf(evaluated.subject),
                        )
                    }
                    val scenarios = scenariosByContract[reference].orEmpty()
                    if (scenarios.isEmpty()) {
                        obligations += MissingFeatureExecutionContractScenarioObligation(
                            responsibleOwner = evaluated.subject.participantOwner,
                            contract = reference,
                            affectedSubjects = listOf(evaluated.subject),
                        )
                    }
                    scenarios.forEach { scenario ->
                        val fixtures = contract.fixtures(graph, evaluated.subject)
                        val evidence = try {
                            scenario.scenario.evidenceFactory.create(
                                FeatureContractScenarioInput(
                                    evaluated.matchedProviders,
                                    evaluated.suppliedAdapters,
                                    fixtures,
                                ),
                            )
                        } catch (error: Throwable) {
                            obligations +=
                                scenario.invalid(evaluated.subject, "evidence factory failed: ${error.message}")
                            return@forEach
                        }
                        when (
                            val resolved = try {
                                resolveFeatureExecutionContext(evaluated, evidence)
                            } catch (error: Throwable) {
                                obligations +=
                                    scenario.invalid(evaluated.subject, "evidence was rejected: ${error.message}")
                                return@forEach
                            }
                        ) {
                            is ApplicableFeatureExecutionContext -> selectExecutionContract(
                                graph = graph,
                                subject = resolved.subject,
                                contract = contract,
                                matchedProviders = evaluated.matchedProviders,
                                suppliedAdapters = evaluated.suppliedAdapters,
                                evidence = resolved.evidence,
                                verifier = verifier,
                                scenario = scenario,
                                obligations = obligations,
                                executions = executions,
                            )
                            is IncompleteFeatureExecutionContext -> graphObligations += resolved.obligations
                            is MissingFeatureExecutionContextEvidence -> obligations += scenario.invalid(
                                evaluated.subject,
                                "missing evidence: ${resolved.missingInputs.joinToString { it.id.value }}",
                            )
                            is BlockedFeatureExecutionContext -> obligations += scenario.invalid(
                                evaluated.subject,
                                "blocked by: ${resolved.blockers.joinToString { it.id.value }}",
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    return FeatureExecutionContractValidationPlanPart(
        executions = executions.sortedWith(
            compareBy(
                { it.contractSelection.subject.sortKey() },
                { it.contractSelection.contract.id.value },
                { it.scenario?.scenario?.id?.value.orEmpty() },
            ),
        ),
        graphObligations = graphObligations.distinct(),
        obligations = obligations.normalized(),
    )
}

private fun selectExecutionContract(
    graph: FeatureGraph,
    subject: FeatureExecutionParticipantSubject,
    contract: FeatureBehaviorContract,
    matchedProviders: List<CapabilityProvider<*>>,
    suppliedAdapters: List<SpecializedAdapter<*>>,
    evidence: List<ContextEvidence<*>>,
    verifier: OwnedFeatureExecutionContractVerifier?,
    scenario: OwnedFeatureExecutionContractScenario?,
    obligations: MutableList<FeatureContractValidationObligation>,
    executions: MutableList<FeatureExecutionContractExecutionSelection>,
) {
    val reference = contract.reference(subject)
    if (verifier == null) {
        obligations += MissingFeatureExecutionContractVerifierObligation(
            responsibleOwner = subject.participantOwner,
            contract = reference,
            affectedSubjects = listOf(subject),
        )
        return
    }
    val fixtures = contract.fixtures(graph, subject)
    val suppliedFixtureIds = fixtures.mapTo(mutableSetOf()) { it.definition.id }
    val missing = contract.fixtureRequirements.filter { it.id !in suppliedFixtureIds }
    if (missing.isNotEmpty()) {
        missing.forEach { requirement ->
            obligations += MissingFeatureExecutionContractFixtureObligation(
                responsibleOwner = subject.contentTypeOwner,
                subject = subject,
                requirement = requirement,
                affectedContracts = listOf(contract),
            )
        }
        return
    }
    executions += FeatureExecutionContractExecutionSelection(
        contractSelection = FeatureExecutionBehavioralContractSelection(
            subject = subject,
            contract = contract,
            matchedProviders = matchedProviders,
            suppliedAdapters = suppliedAdapters,
            fixtures = fixtures,
            contextEvidence = evidence,
        ),
        scenario = scenario,
        verifier = verifier,
    )
}

private fun validateExecutionContractContributions(
    graph: FeatureGraph,
    declarations: Map<FeatureExecutionContractReference, DeclaredExecutionContract>,
    contributions: DiscoveredFeatureValidationContributions,
) {
    contributions.executionVerifiers.groupBy { it.verifier.contract }
        .filterValues { it.size > 1 }
        .forEach { (contract, duplicates) ->
            error("Duplicate execution verifier for $contract from ${duplicates.map { it.owner }}")
        }
    contributions.executionVerifiers.forEach { owned ->
        val declaration = requireNotNull(declarations[owned.verifier.contract]) {
            "Execution verifier ${owned.verifier.contract} has no production behavioral contract"
        }
        require(owned.owner == declaration.owner) {
            "Execution verifier ${owned.verifier.contract} is owned by ${owned.owner}, expected ${declaration.owner}"
        }
    }
    contributions.executionScenarios.groupBy { it.scenario.contract to it.scenario.id }
        .filterValues { it.size > 1 }
        .forEach { (key, duplicates) ->
            error("Duplicate execution contract scenario $key from ${duplicates.map { it.owner }}")
        }
    contributions.executionScenarios.forEach { owned ->
        val declaration = requireNotNull(declarations[owned.scenario.contract]) {
            "Execution scenario ${owned.scenario.id} targets unknown contract ${owned.scenario.contract}"
        }
        require(owned.owner == declaration.owner) {
            "Execution scenario ${owned.scenario.id} is owned by ${owned.owner}, expected ${declaration.owner}"
        }
        val participant = graph.executionParticipants.single { it.id == owned.scenario.contract.participant }
        require(participant.contextInputs.isNotEmpty()) {
            "Execution scenario ${owned.scenario.id} targets context-free participant ${participant.id}"
        }
        require(participant.behavioralContracts.any { it === owned.scenario.contract.contract }) {
            "Execution scenario ${owned.scenario.id} targets a contract not declared by ${participant.id}"
        }
    }
}

private data class DeclaredExecutionContract(
    val owner: mihon.feature.graph.ContributionOwner,
    val contract: FeatureBehaviorContract,
)

private fun FeatureBehaviorContract.reference(
    subject: FeatureExecutionParticipantSubject,
): FeatureExecutionContractReference = FeatureExecutionContractReference(subject.participant, this)

private fun FeatureBehaviorContract.fixtures(
    graph: FeatureGraph,
    subject: FeatureExecutionParticipantSubject,
): List<ContractFixture<*>> {
    val supplied = graph.contentTypes.single { it.contentType == subject.contentType }.contractFixtures
        .associateBy { it.definition.id }
    return fixtureRequirements.sortedBy { it.id.value }.mapNotNull { supplied[it.id] }
}

private fun OwnedFeatureExecutionContractScenario.invalid(
    subject: FeatureExecutionParticipantSubject,
    reason: String,
): InvalidFeatureExecutionContractScenarioObligation {
    return InvalidFeatureExecutionContractScenarioObligation(owner, subject, this, reason)
}

private fun FeatureExecutionParticipantSubject.sortKey(): String {
    return "${contentType.value}:${point.value}:${participant.value}"
}

private fun List<FeatureContractValidationObligation>.normalized(): List<FeatureContractValidationObligation> {
    val missingVerifiers = filterIsInstance<MissingFeatureExecutionContractVerifierObligation>()
        .groupBy { it.contract }
        .values
        .map { matching ->
            matching.first().copy(
                affectedSubjects = matching.flatMap { it.affectedSubjects }
                    .distinct()
                    .sortedBy(FeatureExecutionParticipantSubject::sortKey),
            )
        }
    val missingScenarios = filterIsInstance<MissingFeatureExecutionContractScenarioObligation>()
        .groupBy { it.contract }
        .values
        .map { matching ->
            matching.first().copy(
                affectedSubjects = matching.flatMap { it.affectedSubjects }
                    .distinct()
                    .sortedBy(FeatureExecutionParticipantSubject::sortKey),
            )
        }
    return filterNot {
        it is MissingFeatureExecutionContractVerifierObligation ||
            it is MissingFeatureExecutionContractScenarioObligation
    }.distinct() + missingVerifiers + missingScenarios
}
