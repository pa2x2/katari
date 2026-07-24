package mihon.feature.graph.validation

import mihon.feature.graph.ApplicableFeatureContext
import mihon.feature.graph.BehavioralContractSelection
import mihon.feature.graph.BlockedFeatureContext
import mihon.feature.graph.ConditionalFeatureIntegration
import mihon.feature.graph.ContractFixture
import mihon.feature.graph.ContractFixtureDefinition
import mihon.feature.graph.FeatureArtifactObligation
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContextEvaluation
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.FeatureObligation
import mihon.feature.graph.IncompleteFeatureContext
import mihon.feature.graph.MissingContractFixtureObligation
import mihon.feature.graph.MissingFeatureContextEvidence
import mihon.feature.graph.MissingFeatureProjectionObligation
import mihon.feature.graph.SpecializedFeatureObligation
import mihon.feature.graph.resolveFeatureContext
import mihon.feature.graph.selectContextualFeatureArtifacts
import mihon.feature.graph.selectFeatureArtifacts

data class FeatureContractValidationPlan(
    val executions: List<FeatureContractExecutionSelection>,
    val executionParticipantExecutions: List<FeatureExecutionContractExecutionSelection>,
    val issues: List<FeatureContractPlanIssue>,
) {
    val isComplete: Boolean
        get() = issues.isEmpty()
}

sealed interface FeatureContractPlanIssue {
    val responsibleOwner: mihon.feature.graph.ContributionOwner
}

data class GraphFeatureContractPlanIssue(
    val obligation: FeatureObligation,
) : FeatureContractPlanIssue {
    override val responsibleOwner = obligation.responsibleOwner
}

data class ValidationFeatureContractPlanIssue(
    val obligation: FeatureContractValidationObligation,
) : FeatureContractPlanIssue {
    override val responsibleOwner = obligation.responsibleOwner
}

sealed interface FeatureContractValidationObligation {
    val responsibleOwner: mihon.feature.graph.ContributionOwner
}

data class MissingFeatureContractVerifierObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val contract: FeatureContractReference,
    val affectedSubjects: List<FeatureIntegrationSubject>,
) : FeatureContractValidationObligation

data class MissingFeatureContractScenarioObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val contract: FeatureContractReference,
    val integration: mihon.feature.graph.FeatureIntegrationId,
    val affectedSubjects: List<FeatureIntegrationSubject>,
) : FeatureContractValidationObligation

data class InvalidFeatureContractScenarioObligation(
    override val responsibleOwner: mihon.feature.graph.ContributionOwner,
    val subject: FeatureIntegrationSubject,
    val scenario: OwnedFeatureContractScenario,
    val reason: String,
) : FeatureContractValidationObligation

fun discoverAndPlanFeatureContractValidation(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): FeatureContractValidationPlan {
    return planFeatureContractValidation(
        graph = graph,
        evaluation = evaluation,
        contributions = discoverFeatureValidationContributions(loadFeatureValidationContributors(classLoader)),
    )
}

fun planFeatureContractValidation(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    contributors: Iterable<FeatureValidationContributor>,
): FeatureContractValidationPlan {
    return planFeatureContractValidation(
        graph,
        evaluation,
        discoverFeatureValidationContributions(contributors),
    )
}

fun planFeatureContractValidation(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    contributions: DiscoveredFeatureValidationContributions,
): FeatureContractValidationPlan {
    val declaredContracts = graph.declaredContracts()
    validateContributions(graph, declaredContracts, contributions)
    val executionPlan = planFeatureExecutionContractValidation(graph, evaluation, contributions)
    val verifiersByContract = contributions.verifiers.associateBy { it.verifier.contract }
    val scenariosByContractIntegration = contributions.scenarios.groupBy {
        it.scenario.contract to it.scenario.integration
    }
    val staticArtifacts = selectFeatureArtifacts(graph, evaluation)
    val conditional = evaluation.integrations.filterIsInstance<ConditionalFeatureIntegration>()
    val graphObligations = mutableListOf<FeatureObligation>().apply {
        addAll(evaluation.obligations)
        addAll(evaluation.executionObligations)
        addAll(staticArtifacts.obligations)
        addAll(executionPlan.graphObligations)
    }
    val validationObligations = mutableListOf<FeatureContractValidationObligation>()

    val requiredSubjects = buildMap<FeatureContractReference, MutableList<FeatureIntegrationSubject>> {
        staticArtifacts.behavioralContracts.forEach { selection ->
            getOrPut(selection.reference()) { mutableListOf() } += selection.subject
        }
        conditional.forEach { candidate ->
            candidate.integration.behavioralContracts.forEach { contract ->
                getOrPut(contract.reference(candidate.subject)) { mutableListOf() } += candidate.subject
            }
        }
    }
    requiredSubjects.forEach { (contract, subjects) ->
        if (contract !in verifiersByContract) {
            validationObligations += MissingFeatureContractVerifierObligation(
                responsibleOwner = declaredContracts.getValue(contract).owner,
                contract = contract,
                affectedSubjects = subjects.distinct().sortedBy(FeatureIntegrationSubject::sortKey),
            )
        }
    }

    val executions = mutableListOf<FeatureContractExecutionSelection>()
    val missingScenarioCandidates = mutableListOf<MissingScenarioCandidate>()
    staticArtifacts.behavioralContracts.forEach { selection ->
        val verifier = verifiersByContract[selection.reference()] ?: return@forEach
        if (selection.hasEveryFixture()) {
            executions += FeatureContractExecutionSelection(selection, scenario = null, verifier)
        }
    }

    val contextualFixtureCandidates = mutableListOf<MissingContextualFixtureCandidate>()
    conditional.forEach { candidate ->
        val contentType = graph.contentTypes.single { it.contentType == candidate.subject.contentType }
        candidate.integration.behavioralContracts.forEach contractLoop@{ contract ->
            val reference = contract.reference(candidate.subject)
            val scenarios = scenariosByContractIntegration[reference to candidate.integration.id].orEmpty()
            if (scenarios.isEmpty()) {
                missingScenarioCandidates += MissingScenarioCandidate(candidate.subject, reference)
                return@contractLoop
            }
            val fixtures = contract.selectedFixtures(contentType.contractFixtures)
            val suppliedFixtureIds = fixtures.mapTo(mutableSetOf()) { it.definition.id }
            contract.fixtureRequirements
                .filter { it.id !in suppliedFixtureIds }
                .forEach { requirement ->
                    contextualFixtureCandidates +=
                        MissingContextualFixtureCandidate(candidate.subject, contract, requirement)
                }
            if (fixtures.size != contract.fixtureRequirements.size) return@contractLoop

            scenarios.forEach { scenario ->
                val evidence = try {
                    scenario.scenario.evidenceFactory.create(
                        FeatureContractScenarioInput(candidate.matchedProviders, candidate.suppliedAdapters, fixtures),
                    )
                } catch (error: Throwable) {
                    validationObligations += scenario.invalid(
                        candidate.subject,
                        "evidence factory failed: ${error.message}",
                    )
                    return@forEach
                }
                val resolved = try {
                    resolveFeatureContext(
                        evaluation = evaluation,
                        contentType = candidate.subject.contentType,
                        feature = candidate.subject.feature,
                        integration = candidate.subject.integration,
                        evidence = evidence,
                    )
                } catch (error: Throwable) {
                    validationObligations += scenario.invalid(
                        candidate.subject,
                        "evidence was rejected: ${error.message}",
                    )
                    return@forEach
                }
                when (val result = resolved.integration) {
                    is ApplicableFeatureContext -> {
                        val selectedArtifacts = selectContextualFeatureArtifacts(graph, evaluation, resolved)
                        graphObligations += selectedArtifacts.obligations
                        val selected = selectedArtifacts.behavioralContracts
                            .single { it.contract.id == contract.id }
                        val verifier = verifiersByContract[reference]
                        if (verifier != null) {
                            executions += FeatureContractExecutionSelection(selected, scenario, verifier)
                        }
                    }
                    is IncompleteFeatureContext -> graphObligations += resolved.obligations
                    is MissingFeatureContextEvidence -> validationObligations += scenario.invalid(
                        candidate.subject,
                        "missing evidence: ${result.missingInputs.joinToString { it.id.value }}",
                    )
                    is BlockedFeatureContext -> validationObligations += scenario.invalid(
                        candidate.subject,
                        "blocked by: ${result.blockers.joinToString { it.id.value }}",
                    )
                }
            }
        }
    }
    validationObligations += missingScenarioCandidates.toScenarioObligations()
    graphObligations += contextualFixtureCandidates.toFixtureObligations()

    return FeatureContractValidationPlan(
        executions = executions.sortedWith(
            compareBy(
                { it.contractSelection.subject.sortKey() },
                { it.contractSelection.contract.id.value },
                { it.scenario?.scenario?.id?.value.orEmpty() },
            ),
        ),
        executionParticipantExecutions = executionPlan.executions,
        issues = buildList {
            graphObligations.normalized().mapTo(this, ::GraphFeatureContractPlanIssue)
            validationObligations.distinct().mapTo(this, ::ValidationFeatureContractPlanIssue)
            executionPlan.obligations.mapTo(this) { ValidationFeatureContractPlanIssue(it) }
        },
    )
}

private data class DeclaredContract(
    val owner: mihon.feature.graph.ContributionOwner,
    val definition: FeatureBehaviorContract,
)

private fun FeatureGraph.declaredContracts(): Map<FeatureContractReference, DeclaredContract> {
    val declarations = features
        .flatMap { feature ->
            feature.integrations.flatMap { integration ->
                integration.behavioralContracts.map { contract ->
                    contract.reference(feature.feature) to DeclaredContract(feature.owner, contract)
                }
            }
        }
    declarations
        .groupBy { (reference) -> reference.feature to reference.contract.id }
        .forEach { (identity, matchingDeclarations) ->
            val first = matchingDeclarations.first().first.contract
            check(matchingDeclarations.all { (reference) -> reference.contract === first }) {
                "Behavioral contract $identity must reuse one exact definition"
            }
        }
    return declarations.associate { it }
}

private fun validateContributions(
    graph: FeatureGraph,
    contracts: Map<FeatureContractReference, DeclaredContract>,
    contributions: DiscoveredFeatureValidationContributions,
) {
    contributions.verifiers.groupBy { it.verifier.contract }
        .filterValues { it.size > 1 }
        .forEach { (contract, duplicates) ->
            error("Duplicate verifier for $contract from ${duplicates.map { it.owner }.sortedBy { it.value }}")
        }
    contributions.verifiers.forEach { owned ->
        val declaration = requireNotNull(contracts[owned.verifier.contract]) {
            "Verifier ${owned.verifier.contract} has no production behavioral contract"
        }
        require(owned.owner == declaration.owner) {
            "Verifier ${owned.verifier.contract} is owned by ${owned.owner}, expected ${declaration.owner}"
        }
    }

    contributions.scenarios.groupBy { Triple(it.scenario.contract, it.scenario.integration, it.scenario.id) }
        .filterValues { it.size > 1 }
        .forEach { (key, duplicates) ->
            error("Duplicate contract scenario $key from ${duplicates.map { it.owner }.sortedBy { it.value }}")
        }
    contributions.scenarios.forEach { owned ->
        val declaration = requireNotNull(contracts[owned.scenario.contract]) {
            "Scenario ${owned.scenario.id} targets unknown contract ${owned.scenario.contract}"
        }
        require(owned.owner == declaration.owner) {
            "Scenario ${owned.scenario.id} is owned by ${owned.owner}, expected ${declaration.owner}"
        }
        val feature = graph.features.single { it.feature == owned.scenario.contract.feature }
        val integration = feature.integrations.singleOrNull { it.id == owned.scenario.integration }
            ?: error("Scenario ${owned.scenario.id} targets unknown integration ${owned.scenario.integration}")
        require(integration.contextInputs.isNotEmpty()) {
            "Scenario ${owned.scenario.id} targets context-free integration ${integration.id}"
        }
        require(integration.behavioralContracts.any { it === owned.scenario.contract.contract }) {
            "Scenario ${owned.scenario.id} targets a contract not declared by integration ${integration.id}"
        }
    }
}

private fun BehavioralContractSelection.reference(): FeatureContractReference =
    contract.reference(subject)

private fun FeatureBehaviorContract.reference(subject: FeatureIntegrationSubject): FeatureContractReference =
    reference(subject.feature)

private fun FeatureBehaviorContract.reference(feature: mihon.feature.graph.FeatureId): FeatureContractReference =
    FeatureContractReference(feature, this)

private fun BehavioralContractSelection.hasEveryFixture(): Boolean {
    val supplied = fixtures.mapTo(mutableSetOf()) { it.definition.id }
    return contract.fixtureRequirements.all { it.id in supplied }
}

private fun FeatureBehaviorContract.selectedFixtures(
    supplied: List<ContractFixture<*>>,
): List<ContractFixture<*>> {
    val suppliedById = supplied.associateBy { it.definition.id }
    return fixtureRequirements.sortedBy { it.id.value }.mapNotNull { suppliedById[it.id] }
}

private fun OwnedFeatureContractScenario.invalid(
    subject: FeatureIntegrationSubject,
    reason: String,
): InvalidFeatureContractScenarioObligation = InvalidFeatureContractScenarioObligation(
    responsibleOwner = owner,
    subject = subject,
    scenario = this,
    reason = reason,
)

private data class MissingContextualFixtureCandidate(
    val subject: FeatureIntegrationSubject,
    val contract: FeatureBehaviorContract,
    val requirement: ContractFixtureDefinition<*>,
)

private data class MissingScenarioCandidate(
    val subject: FeatureIntegrationSubject,
    val contract: FeatureContractReference,
)

private fun List<MissingScenarioCandidate>.toScenarioObligations(): List<FeatureContractValidationObligation> {
    return groupBy { it.contract to it.subject.integration }.values.map { candidates ->
        val first = candidates.first()
        MissingFeatureContractScenarioObligation(
            responsibleOwner = first.subject.featureOwner,
            contract = first.contract,
            integration = first.subject.integration,
            affectedSubjects = candidates.map { it.subject }.distinct().sortedBy(FeatureIntegrationSubject::sortKey),
        )
    }
}

private fun List<MissingContextualFixtureCandidate>.toFixtureObligations(): List<FeatureArtifactObligation> {
    return groupBy { it.subject to it.requirement.id }.values.map { candidates ->
        val first = candidates.first()
        MissingContractFixtureObligation(
            responsibleOwner = first.subject.contentTypeOwner,
            subject = first.subject,
            requirement = first.requirement,
            affectedContracts = candidates.map { it.contract }.distinct().sortedBy { it.id.value },
        )
    }
}

private fun List<FeatureObligation>.normalized(): List<FeatureObligation> {
    val projectionObligations = filterIsInstance<MissingFeatureProjectionObligation>()
        .groupBy { obligation ->
            Triple(obligation.feature, obligation.integration, obligation.requirement.id)
        }
        .values
        .map { matching ->
            val first = matching.first()
            first.copy(
                affectedSubjects = matching
                    .flatMap { it.affectedSubjects }
                    .distinct()
                    .sortedBy(FeatureIntegrationSubject::sortKey),
            )
        }
        .sortedWith(compareBy({ it.feature.value }, { it.integration.value }, { it.requirement.id.value }))
    return filterNot { it is MissingFeatureProjectionObligation }.distinct() + projectionObligations
}

private fun FeatureIntegrationSubject.sortKey(): String =
    "${contentType.value}:${feature.value}:${integration.value}"
