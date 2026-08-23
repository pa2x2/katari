package mihon.feature.graph.validation.reporting

import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.graph.ApplicationSubjectContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ConditionalFeatureIntegration
import mihon.feature.graph.FeatureArtifactObligation
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureIntegrationEvaluation
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.FeatureSubjectReference
import mihon.feature.graph.InapplicableFeatureIntegration
import mihon.feature.graph.IncompleteFeatureIntegration
import mihon.feature.graph.MissingContractFixtureObligation
import mihon.feature.graph.MissingFeatureProjectionObligation
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.SpecializedAdapterDefinition
import mihon.feature.graph.SpecializedExecutionParticipantObligation
import mihon.feature.graph.SpecializedFeatureObligation
import mihon.feature.graph.execution.ApplicableFeatureExecutionParticipant
import mihon.feature.graph.execution.ConditionalFeatureExecutionParticipant
import mihon.feature.graph.execution.FeatureExecutionParticipantSubject
import mihon.feature.graph.execution.InapplicableFeatureExecutionParticipant
import mihon.feature.graph.execution.IncompleteFeatureExecutionParticipant
import mihon.feature.graph.validation.CompletedFeatureContractExecution
import mihon.feature.graph.validation.CompletedFeatureExecutionContractExecution
import mihon.feature.graph.validation.CrashedFeatureContractExecution
import mihon.feature.graph.validation.CrashedFeatureExecutionContractExecution
import mihon.feature.graph.validation.FeatureContractExecutionResult
import mihon.feature.graph.validation.FeatureContractValidationResult
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureExecutionContractExecutionResult
import mihon.feature.graph.validation.GraphFeatureContractPlanIssue
import mihon.feature.graph.validation.InvalidFeatureContractScenarioObligation
import mihon.feature.graph.validation.InvalidFeatureExecutionContractScenarioObligation
import mihon.feature.graph.validation.MissingFeatureContractScenarioObligation
import mihon.feature.graph.validation.MissingFeatureContractVerifierObligation
import mihon.feature.graph.validation.MissingFeatureExecutionContractFixtureObligation
import mihon.feature.graph.validation.MissingFeatureExecutionContractScenarioObligation
import mihon.feature.graph.validation.MissingFeatureExecutionContractVerifierObligation
import mihon.feature.graph.validation.ValidationFeatureContractPlanIssue
import mihon.feature.graph.validation.entryContentTypes

fun buildFeatureDeveloperReport(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    validation: FeatureContractValidationResult,
): FeatureDeveloperReport {
    val executions = validation.executions.groupBy { execution ->
        val selection = execution.selection.contractSelection
        ContractKey(selection.subject, selection.contract.id.value)
    }
    val invalidScenarios = validation.plan.issues
        .filterIsInstance<ValidationFeatureContractPlanIssue>()
        .map { it.obligation }
        .filterIsInstance<InvalidFeatureContractScenarioObligation>()
        .groupBy { obligation ->
            ContractKey(obligation.subject, obligation.scenario.scenario.contract.contract.id.value)
        }
    val executionContractExecutions = validation.executionParticipantExecutions.groupBy { execution ->
        val selection = execution.selection.contractSelection
        ExecutionContractKey(selection.subject, selection.contract.id.value)
    }
    val invalidExecutionScenarios = validation.plan.issues
        .filterIsInstance<ValidationFeatureContractPlanIssue>()
        .map { it.obligation }
        .filterIsInstance<InvalidFeatureExecutionContractScenarioObligation>()
        .groupBy { obligation ->
            ExecutionContractKey(
                obligation.subject,
                obligation.scenario.scenario.contract.contract.id.value,
            )
        }

    return FeatureDeveloperReport(
        application = graph.subjects.filterIsInstance<ApplicationSubjectContribution>()
            .singleOrNull()
            ?.let { contribution ->
                FeatureDeveloperApplication(
                    owner = contribution.owner.value,
                    providers = contribution.providers
                        .map { it.reference() }
                        .sortedBy(FeatureDeveloperOwnedReference::id),
                    specializedAdapters = contribution.specializedAdapters
                        .map { it.reference() }
                        .sortedBy(FeatureDeveloperOwnedReference::id),
                    contractFixtures = contribution.contractFixtures
                        .map { fixture ->
                            FeatureDeveloperOwnedReference(
                                id = fixture.definition.id.value,
                                owner = fixture.definition.owner.value,
                            )
                        }
                        .sortedBy(FeatureDeveloperOwnedReference::id),
                )
            },
        contentTypes = graph.entryContentTypes.map { contribution ->
            FeatureDeveloperContentType(
                id = contribution.contentType.value,
                owner = contribution.owner.value,
                providers = contribution.providers
                    .map { it.reference() }
                    .sortedBy(FeatureDeveloperOwnedReference::id),
                specializedAdapters = contribution.specializedAdapters
                    .map { it.reference() }
                    .sortedBy(FeatureDeveloperOwnedReference::id),
                contractFixtures = contribution.contractFixtures
                    .map { fixture ->
                        FeatureDeveloperOwnedReference(
                            id = fixture.definition.id.value,
                            owner = fixture.definition.owner.value,
                        )
                    }
                    .sortedBy(FeatureDeveloperOwnedReference::id),
            )
        },
        features = graph.features.map { contribution ->
            FeatureDeveloperFeature(
                id = contribution.feature.value,
                owner = contribution.owner.value,
            )
        },
        executionPoints = graph.executionPoints.map { point ->
            FeatureDeveloperExecutionPoint(
                id = point.id.value,
                owner = point.owner.value,
                eventType = point.eventType.qualifiedName ?: point.eventType.toString(),
                phase = point.phase.id,
                failurePolicy = point.failurePolicy.name.lowercase(),
            )
        },
        executionParticipants = evaluation.executionParticipants
            .sortedBy {
                "${it.subject.affectedSubject.id.stableValue}:" +
                    "${it.subject.point.value}:${it.subject.participant.value}"
            }
            .map { evaluated ->
                val state = when (evaluated) {
                    is InapplicableFeatureExecutionParticipant -> FeatureDeveloperIntegrationState.INAPPLICABLE
                    is ConditionalFeatureExecutionParticipant -> FeatureDeveloperIntegrationState.CONDITIONAL
                    is IncompleteFeatureExecutionParticipant -> FeatureDeveloperIntegrationState.INCOMPLETE
                    is ApplicableFeatureExecutionParticipant -> FeatureDeveloperIntegrationState.APPLICABLE
                }
                FeatureDeveloperExecutionParticipant(
                    subject = evaluated.subject.affectedSubject.report(),
                    point = FeatureDeveloperOwnedReference(
                        evaluated.participant.point.id.value,
                        evaluated.participant.point.owner.value,
                    ),
                    participant = FeatureDeveloperOwnedReference(
                        evaluated.participant.id.value,
                        evaluated.participant.owner.value,
                    ),
                    state = state,
                    prerequisites = evaluated.participant.prerequisites.report(),
                    contextInputs = evaluated.participant.contextInputs.map { input ->
                        FeatureDeveloperOwnedReference(input.id.value, input.owner.value)
                    }.sortedBy(FeatureDeveloperOwnedReference::id),
                    after = evaluated.participant.order.after.map { it.value }.sorted(),
                    afterIfPresent = evaluated.participant.order.afterIfPresent.map { it.value }.sorted(),
                    before = evaluated.participant.order.before.map { it.value }.sorted(),
                    contracts = evaluated.participant.behavioralContracts.map { contract ->
                        val key = ExecutionContractKey(evaluated.subject, contract.id.value)
                        FeatureDeveloperExecutionContract(
                            id = contract.id.value,
                            validations = buildList {
                                executionContractExecutions[key].orEmpty().mapTo(this) { it.report() }
                                invalidExecutionScenarios[key].orEmpty().mapTo(this) { invalid ->
                                    FeatureDeveloperContractValidation(
                                        scenario = invalid.scenario.scenario.id.value,
                                        outcome = FeatureDeveloperContractValidationOutcome.INVALID_SCENARIO,
                                        details = listOf(invalid.reason),
                                    )
                                }
                            },
                        )
                    }.sortedBy(FeatureDeveloperExecutionContract::id),
                )
            },
        integrations = evaluation.integrations
            .sortedBy { it.subject.sortKey() }
            .map { evaluated ->
                evaluated.report(
                    executions = executions,
                    invalidScenarios = invalidScenarios,
                )
            },
        obligations = (
            validation.plan.issues.flatMap { it.report() } +
                evaluation.executionObligations.map { it.report() }
            )
            .sortedWith(
                compareBy(
                    FeatureDeveloperObligation::responsibleOwner,
                    { it.category.name },
                    FeatureDeveloperObligation::artifact,
                    { it.subjects.joinToString { subject -> subject.sortKey() } },
                ),
            ),
    )
}

private fun FeatureIntegrationEvaluation.report(
    executions: Map<ContractKey, List<FeatureContractExecutionResult>>,
    invalidScenarios: Map<ContractKey, List<InvalidFeatureContractScenarioObligation>>,
): FeatureDeveloperIntegration {
    val state = when (this) {
        is InapplicableFeatureIntegration -> FeatureDeveloperIntegrationState.INAPPLICABLE
        is ConditionalFeatureIntegration -> FeatureDeveloperIntegrationState.CONDITIONAL
        is IncompleteFeatureIntegration -> FeatureDeveloperIntegrationState.INCOMPLETE
        is ApplicableFeatureIntegration -> FeatureDeveloperIntegrationState.APPLICABLE
    }
    val artifactAvailability = state.artifactAvailability()
    val matchedProviders = when (this) {
        is InapplicableFeatureIntegration -> matchedProviders
        is ConditionalFeatureIntegration -> matchedProviders
        is IncompleteFeatureIntegration -> matchedProviders
        is ApplicableFeatureIntegration -> matchedProviders
    }
    val suppliedAdapters = when (this) {
        is InapplicableFeatureIntegration -> emptyList()
        is ConditionalFeatureIntegration -> suppliedAdapters
        is IncompleteFeatureIntegration -> suppliedAdapters
        is ApplicableFeatureIntegration -> suppliedAdapters
    }
    val pendingRequirements = when (this) {
        is ConditionalFeatureIntegration -> pendingSpecializedRequirements
        is IncompleteFeatureIntegration -> obligations.map { it.requirement }
        is InapplicableFeatureIntegration,
        is ApplicableFeatureIntegration,
        -> emptyList()
    }

    return FeatureDeveloperIntegration(
        subject = subject.affectedSubject.report(),
        feature = FeatureDeveloperOwnedReference(
            id = subject.feature.value,
            owner = subject.featureOwner.value,
        ),
        id = subject.integration.value,
        state = state,
        prerequisites = integration.prerequisites.report(),
        matchedProviders = matchedProviders.map { it.reference() }.sortedBy(FeatureDeveloperOwnedReference::id),
        unmetPrerequisites = (this as? InapplicableFeatureIntegration)
            ?.unmetPrerequisites
            .orEmpty()
            .map { it.report() },
        missingSpecializedPrerequisites = (this as? InapplicableFeatureIntegration)
            ?.unmetSpecializedPrerequisites
            .orEmpty()
            .map { it.reference() }
            .sortedBy(FeatureDeveloperOwnedReference::id),
        suppliedSpecializedAdapters = suppliedAdapters
            .map { it.reference() }
            .sortedBy(FeatureDeveloperOwnedReference::id),
        pendingSpecializedRequirements = pendingRequirements
            .map { it.reference() }
            .sortedBy(FeatureDeveloperOwnedReference::id),
        contextInputs = integration.contextInputs.map { input ->
            FeatureDeveloperOwnedReference(input.id.value, input.owner.value)
        }.sortedBy(FeatureDeveloperOwnedReference::id),
        declaredBlockers = integration.contextBlockers.map { blocker ->
            FeatureDeveloperBlocker(
                id = blocker.id.value,
                inputs = blocker.inputs.map { input ->
                    FeatureDeveloperOwnedReference(input.id.value, input.owner.value)
                }.sortedBy(FeatureDeveloperOwnedReference::id),
            )
        }.sortedBy(FeatureDeveloperBlocker::id),
        behaviors = integration.behaviorProjections.map { behavior ->
            FeatureDeveloperArtifact(behavior.id.value, artifactAvailability)
        }.sortedBy(FeatureDeveloperArtifact::id),
        contracts = integration.behavioralContracts.map { contract ->
            val key = ContractKey(subject, contract.id.value)
            FeatureDeveloperContract(
                id = contract.id.value,
                availability = artifactAvailability,
                fixtureRequirements = contract.fixtureRequirements.map { requirement ->
                    FeatureDeveloperOwnedReference(requirement.id.value, requirement.owner.value)
                }.sortedBy(FeatureDeveloperOwnedReference::id),
                validations = buildList {
                    executions[key].orEmpty().mapTo(this) { it.report() }
                    invalidScenarios[key].orEmpty().mapTo(this) { invalid ->
                        FeatureDeveloperContractValidation(
                            scenario = invalid.scenario.scenario.id.value,
                            outcome = FeatureDeveloperContractValidationOutcome.INVALID_SCENARIO,
                            details = listOf(invalid.reason),
                        )
                    }
                }.sortedWith(compareBy({ it.scenario.orEmpty() }, { it.outcome.name })),
            )
        }.sortedBy(FeatureDeveloperContract::id),
        projections = integration.projectionRequirements.map { requirement ->
            FeatureDeveloperProjection(
                id = requirement.id.value,
                owner = requirement.owner.value,
                implementationPresent = integration.projections.any { projection ->
                    projection.definition.id == requirement.id
                },
                availability = artifactAvailability,
            )
        }.sortedBy(FeatureDeveloperProjection::id),
    )
}

private fun FeatureDeveloperIntegrationState.artifactAvailability(): FeatureDeveloperArtifactAvailability {
    return when (this) {
        FeatureDeveloperIntegrationState.INAPPLICABLE -> FeatureDeveloperArtifactAvailability.NOT_SELECTED
        FeatureDeveloperIntegrationState.CONDITIONAL -> FeatureDeveloperArtifactAvailability.CONDITIONAL
        FeatureDeveloperIntegrationState.INCOMPLETE -> FeatureDeveloperArtifactAvailability.BLOCKED
        FeatureDeveloperIntegrationState.APPLICABLE -> FeatureDeveloperArtifactAvailability.SELECTED
    }
}

private fun FeatureContractExecutionResult.report(): FeatureDeveloperContractValidation {
    val scenario = selection.scenario?.scenario?.id?.value
    return when (this) {
        is CompletedFeatureContractExecution -> when (val result = verification) {
            FeatureContractVerificationResult.Passed -> FeatureDeveloperContractValidation(
                scenario = scenario,
                outcome = FeatureDeveloperContractValidationOutcome.PASSED,
                details = emptyList(),
            )
            is FeatureContractVerificationResult.Failed -> FeatureDeveloperContractValidation(
                scenario = scenario,
                outcome = FeatureDeveloperContractValidationOutcome.FAILED,
                details = result.failures.map { it.message },
            )
        }
        is CrashedFeatureContractExecution -> FeatureDeveloperContractValidation(
            scenario = scenario,
            outcome = FeatureDeveloperContractValidationOutcome.CRASHED,
            details = listOfNotNull(cause::class.qualifiedName, cause.message),
        )
    }
}

private fun FeatureExecutionContractExecutionResult.report(): FeatureDeveloperContractValidation {
    val scenario = selection.scenario?.scenario?.id?.value
    return when (this) {
        is CompletedFeatureExecutionContractExecution -> when (val result = verification) {
            FeatureContractVerificationResult.Passed -> FeatureDeveloperContractValidation(
                scenario = scenario,
                outcome = FeatureDeveloperContractValidationOutcome.PASSED,
                details = emptyList(),
            )
            is FeatureContractVerificationResult.Failed -> FeatureDeveloperContractValidation(
                scenario = scenario,
                outcome = FeatureDeveloperContractValidationOutcome.FAILED,
                details = result.failures.map { it.message },
            )
        }
        is CrashedFeatureExecutionContractExecution -> FeatureDeveloperContractValidation(
            scenario = scenario,
            outcome = FeatureDeveloperContractValidationOutcome.CRASHED,
            details = listOfNotNull(cause::class.qualifiedName, cause.message),
        )
    }
}

private fun mihon.feature.graph.validation.FeatureContractPlanIssue.report(): List<FeatureDeveloperObligation> {
    return when (this) {
        is GraphFeatureContractPlanIssue -> listOf(obligation.report())
        is ValidationFeatureContractPlanIssue -> when (val missing = obligation) {
            is MissingFeatureContractVerifierObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.CONTRACT_VERIFIER,
                    subjects = missing.affectedSubjects.map { it.report() },
                    artifact = missing.contract.contract.id.value,
                    details = "No discovered verifier implements the selected contract",
                ),
            )
            is MissingFeatureContractScenarioObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.CONTRACT_SCENARIO,
                    subjects = missing.affectedSubjects.map { it.report() },
                    artifact = missing.contract.contract.id.value,
                    details = "No discovered scenario establishes applicable context for ${missing.integration.value}",
                ),
            )
            is InvalidFeatureContractScenarioObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.INVALID_CONTRACT_SCENARIO,
                    subjects = listOf(missing.subject.report()),
                    artifact = missing.scenario.scenario.id.value,
                    details = missing.reason,
                ),
            )
            is MissingFeatureExecutionContractVerifierObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.CONTRACT_VERIFIER,
                    subjects = missing.affectedSubjects.map { it.report() },
                    artifact = missing.contract.contract.id.value,
                    details = "No discovered verifier implements the selected execution-participant contract",
                ),
            )
            is MissingFeatureExecutionContractScenarioObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.CONTRACT_SCENARIO,
                    subjects = missing.affectedSubjects.map { it.report() },
                    artifact = missing.contract.contract.id.value,
                    details = "No discovered scenario establishes applicable execution context",
                ),
            )
            is InvalidFeatureExecutionContractScenarioObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.INVALID_CONTRACT_SCENARIO,
                    subjects = listOf(missing.subject.report()),
                    artifact = missing.scenario.scenario.id.value,
                    details = missing.reason,
                ),
            )
            is MissingFeatureExecutionContractFixtureObligation -> listOf(
                FeatureDeveloperObligation(
                    responsibleOwner = missing.responsibleOwner.value,
                    category = FeatureDeveloperObligationCategory.CONTRACT_FIXTURE,
                    subjects = listOf(missing.subject.report()),
                    artifact = missing.requirement.id.value,
                    details = "Missing fixture for execution contracts: " +
                        missing.affectedContracts.joinToString { it.id.value },
                ),
            )
        }
    }
}

private fun mihon.feature.graph.FeatureObligation.report(): FeatureDeveloperObligation {
    return when (this) {
        is SpecializedFeatureObligation -> FeatureDeveloperObligation(
            responsibleOwner = responsibleOwner.value,
            category = FeatureDeveloperObligationCategory.SPECIALIZED_ADAPTER,
            subjects = listOf(subject.report()),
            artifact = requirement.id.value,
            details = "Applicable integration requires a specialized adapter",
        )
        is SpecializedExecutionParticipantObligation -> report()
        is FeatureArtifactObligation -> report()
    }
}

private fun SpecializedExecutionParticipantObligation.report(): FeatureDeveloperObligation {
    return FeatureDeveloperObligation(
        responsibleOwner = responsibleOwner.value,
        category = FeatureDeveloperObligationCategory.EXECUTION_SPECIALIZED_ADAPTER,
        subjects = listOf(
            FeatureDeveloperEvaluationSubject(
                subject = subject.affectedSubject.report(),
                feature = "execution.${subject.point.value}",
                integration = subject.participant.value,
            ),
        ),
        artifact = requirement.id.value,
        details = "Applicable execution participant requires a specialized adapter",
    )
}

private fun FeatureArtifactObligation.report(): FeatureDeveloperObligation {
    return when (this) {
        is MissingContractFixtureObligation -> FeatureDeveloperObligation(
            responsibleOwner = responsibleOwner.value,
            category = FeatureDeveloperObligationCategory.CONTRACT_FIXTURE,
            subjects = listOf(subject.report()),
            artifact = requirement.id.value,
            details = "Missing fixture for contracts: ${affectedContracts.joinToString { it.id.value }}",
        )
        is MissingFeatureProjectionObligation -> FeatureDeveloperObligation(
            responsibleOwner = responsibleOwner.value,
            category = FeatureDeveloperObligationCategory.PROJECTION,
            subjects = affectedSubjects.map { it.report() },
            artifact = requirement.id.value,
            details = "Applicable integration has no projection implementation",
        )
    }
}

private fun CapabilityExpression.report(): FeatureDeveloperCapabilityRequirement {
    return when (this) {
        CapabilityExpression.Always -> FeatureDeveloperCapabilityRequirement.Always
        is CapabilityExpression.Provided -> FeatureDeveloperCapabilityRequirement.Provided(
            FeatureDeveloperOwnedReference(capability.id.value, capability.owner.value),
        )
        is CapabilityExpression.AllOf -> FeatureDeveloperCapabilityRequirement.AllOf(terms.map { it.report() })
        is CapabilityExpression.AnyOf -> FeatureDeveloperCapabilityRequirement.AnyOf(terms.map { it.report() })
    }
}

private fun CapabilityProvider<*>.reference(): FeatureDeveloperOwnedReference {
    return FeatureDeveloperOwnedReference(capability.id.value, capability.owner.value)
}

private fun SpecializedAdapter<*>.reference(): FeatureDeveloperOwnedReference = definition.reference()

private fun SpecializedAdapterDefinition<*>.reference(): FeatureDeveloperOwnedReference {
    return FeatureDeveloperOwnedReference(id.value, owner.value)
}

private fun FeatureIntegrationSubject.report(): FeatureDeveloperEvaluationSubject {
    return FeatureDeveloperEvaluationSubject(
        subject = affectedSubject.report(),
        feature = feature.value,
        integration = integration.value,
    )
}

private fun FeatureExecutionParticipantSubject.report(): FeatureDeveloperEvaluationSubject {
    return FeatureDeveloperEvaluationSubject(
        subject = affectedSubject.report(),
        feature = "execution.${point.value}",
        integration = participant.value,
    )
}

private fun FeatureIntegrationSubject.sortKey(): String =
    "${affectedSubject.id.stableValue}:${feature.value}:${integration.value}"

private fun FeatureDeveloperEvaluationSubject.sortKey(): String = "${subject.id}:$feature:$integration"

private fun FeatureSubjectReference.report(): FeatureDeveloperSubjectReference {
    val (displayId, scope) = when (val subjectId = id) {
        FeatureSubjectId.Application ->
            "application" to FeatureDeveloperSubjectScope.APPLICATION
        is FeatureSubjectId.EntryContentType ->
            subjectId.contentType.value to FeatureDeveloperSubjectScope.ENTRY_CONTENT_TYPE
    }
    return FeatureDeveloperSubjectReference(
        id = displayId,
        owner = owner.value,
        scope = scope,
    )
}

private data class ContractKey(
    val subject: FeatureIntegrationSubject,
    val contract: String,
)

private data class ExecutionContractKey(
    val subject: FeatureExecutionParticipantSubject,
    val contract: String,
)
