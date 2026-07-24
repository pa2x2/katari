package mihon.feature.graph

/** One feature-owned behavioral contract selected for an applicable content type. */
data class BehavioralContractSelection(
    val subject: FeatureIntegrationSubject,
    val contract: FeatureBehaviorContract,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val fixtures: List<ContractFixture<*>>,
    val contextEvidence: List<ContextEvidence<*>>,
)

/** One feature-owned projection selected for an applicable content type. */
data class FeatureProjectionSelection(
    val subject: FeatureIntegrationSubject,
    val projection: FeatureProjection<*>,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val contextEvidence: List<ContextEvidence<*>>,
)

sealed interface FeatureArtifactObligation : FeatureObligation

/** Missing validation-only input for one or more contracts selected for an affected content type. */
data class MissingContractFixtureObligation(
    override val responsibleOwner: ContributionOwner,
    val subject: FeatureIntegrationSubject,
    val requirement: ContractFixtureDefinition<*>,
    val affectedContracts: List<FeatureBehaviorContract>,
) : FeatureArtifactObligation

/** Missing shared projection implementation, reported once with every applicable subject it affects. */
data class MissingFeatureProjectionObligation(
    override val responsibleOwner: ContributionOwner,
    val feature: FeatureId,
    val integration: FeatureIntegrationId,
    val requirement: FeatureProjectionDefinition<*>,
    val affectedSubjects: List<FeatureIntegrationSubject>,
) : FeatureArtifactObligation

/** Contracts, projections, and their unsatisfied requirements derived from one graph evaluation. */
data class FeatureArtifactSelection(
    val behavioralContracts: List<BehavioralContractSelection>,
    val projections: List<FeatureProjectionSelection>,
    val obligations: List<FeatureArtifactObligation>,
)

/**
 * Selects executable artifacts from already evaluated applicability.
 *
 * This operation never re-evaluates capability expressions. Inapplicable, conditional, and runtime-incomplete
 * integrations select nothing. Missing validation fixtures and feature-owned projections become explicit obligations.
 */
fun selectFeatureArtifacts(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
): FeatureArtifactSelection {
    validateEvaluationCoverage(graph, evaluation)
    val applicable = evaluation.integrations
        .filterIsInstance<ApplicableFeatureIntegration>()
        .map {
            ArtifactApplicability(
                subject = it.subject,
                integration = it.integration,
                matchedProviders = it.matchedProviders,
                suppliedAdapters = it.suppliedAdapters,
                contextEvidence = emptyList(),
            )
        }
    return selectApplicableArtifacts(graph, applicable)
}

/** Selects artifacts activated by one exact runtime context snapshot without treating it as type-wide support. */
fun selectContextualFeatureArtifacts(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    contextEvaluation: FeatureContextEvaluation,
): FeatureArtifactSelection {
    validateEvaluationCoverage(graph, evaluation)
    val resolved = contextEvaluation.integration
    val candidate = evaluation.integrations
        .filterIsInstance<ConditionalFeatureIntegration>()
        .singleOrNull { it.subject == resolved.subject }
        ?: error("Context evaluation ${resolved.subject.describe()} has no matching conditional graph evaluation")
    check(candidate.integration === resolved.integration) {
        "Context evaluation ${resolved.subject.describe()} does not belong to the supplied feature graph"
    }
    val applicable = resolved as? ApplicableFeatureContext ?: return FeatureArtifactSelection(
        behavioralContracts = emptyList(),
        projections = emptyList(),
        obligations = emptyList(),
    )
    return selectApplicableArtifacts(
        graph = graph,
        applicable = listOf(
            ArtifactApplicability(
                subject = applicable.subject,
                integration = applicable.integration,
                matchedProviders = applicable.matchedProviders,
                suppliedAdapters = applicable.suppliedAdapters,
                contextEvidence = applicable.evidence,
            ),
        ),
    )
}

private fun selectApplicableArtifacts(
    graph: FeatureGraph,
    applicable: List<ArtifactApplicability>,
): FeatureArtifactSelection {
    val contentTypesById = graph.contentTypes.associateBy { it.contentType }

    val contractSelections = applicable.flatMap { evaluated ->
        val contentType = contentTypesById.getValue(evaluated.subject.contentType)
        val fixturesById = contentType.contractFixtures.associateBy { it.definition.id }
        evaluated.integration.behavioralContracts
            .sortedBy { it.id.value }
            .map { contract ->
                BehavioralContractSelection(
                    subject = evaluated.subject,
                    contract = contract,
                    matchedProviders = evaluated.matchedProviders,
                    suppliedAdapters = evaluated.suppliedAdapters,
                    fixtures = contract.fixtureRequirements
                        .sortedBy { it.id.value }
                        .mapNotNull { fixturesById[it.id] },
                    contextEvidence = evaluated.contextEvidence,
                )
            }
    }

    val missingFixtureObligations = contractSelections
        .flatMap { selection ->
            val suppliedIds = selection.fixtures.mapTo(mutableSetOf()) { it.definition.id }
            selection.contract.fixtureRequirements
                .filter { it.id !in suppliedIds }
                .map { requirement -> MissingFixtureCandidate(selection, requirement) }
        }
        .groupBy { candidate -> candidate.selection.subject to candidate.requirement.id }
        .values
        .map { candidates ->
            val first = candidates.first()
            MissingContractFixtureObligation(
                responsibleOwner = first.selection.subject.contentTypeOwner,
                subject = first.selection.subject,
                requirement = first.requirement,
                affectedContracts = candidates.map { it.selection.contract }.sortedBy { it.id.value },
            )
        }
        .sortedWith(
            compareBy<MissingContractFixtureObligation>(
                { it.subject.contentType.value },
                { it.subject.feature.value },
                { it.subject.integration.value },
                { it.requirement.id.value },
            ),
        )

    val projectionSelections = applicable.flatMap { evaluated ->
        evaluated.integration.projections
            .sortedBy { it.definition.id.value }
            .map { projection ->
                FeatureProjectionSelection(
                    subject = evaluated.subject,
                    projection = projection,
                    matchedProviders = evaluated.matchedProviders,
                    suppliedAdapters = evaluated.suppliedAdapters,
                    contextEvidence = evaluated.contextEvidence,
                )
            }
    }

    val missingProjectionObligations = applicable
        .flatMap { evaluated ->
            val suppliedIds = evaluated.integration.projections
                .mapTo(mutableSetOf()) { it.definition.id }
            evaluated.integration.projectionRequirements
                .filter { it.id !in suppliedIds }
                .map { requirement -> MissingProjectionCandidate(evaluated.subject, requirement) }
        }
        .groupBy { candidate ->
            ProjectionObligationKey(
                feature = candidate.subject.feature,
                integration = candidate.subject.integration,
                requirement = candidate.requirement.id,
            )
        }
        .values
        .map { candidates ->
            val first = candidates.first()
            MissingFeatureProjectionObligation(
                responsibleOwner = first.subject.featureOwner,
                feature = first.subject.feature,
                integration = first.subject.integration,
                requirement = first.requirement,
                affectedSubjects = candidates.map { it.subject },
            )
        }
        .sortedWith(
            compareBy<MissingFeatureProjectionObligation>(
                { it.feature.value },
                { it.integration.value },
                { it.requirement.id.value },
            ),
        )

    return FeatureArtifactSelection(
        behavioralContracts = contractSelections,
        projections = projectionSelections,
        obligations = missingFixtureObligations + missingProjectionObligations,
    )
}

private data class ArtifactApplicability(
    val subject: FeatureIntegrationSubject,
    val integration: FeatureIntegration,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val contextEvidence: List<ContextEvidence<*>>,
)

private fun validateEvaluationCoverage(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
) {
    val expected = buildMap {
        graph.contentTypes.forEach { contentType ->
            graph.features.forEach { feature ->
                feature.integrations.forEach { integration ->
                    put(
                        FeatureIntegrationSubject(
                            contentType = contentType.contentType,
                            contentTypeOwner = contentType.owner,
                            feature = feature.feature,
                            featureOwner = feature.owner,
                            integration = integration.id,
                        ),
                        integration,
                    )
                }
            }
        }
    }
    val actualBySubject = evaluation.integrations.groupBy { it.subject }
    val duplicates = actualBySubject.filterValues { it.size > 1 }.keys
    check(duplicates.isEmpty()) {
        "Feature graph evaluation contains duplicate subjects: ${duplicates.map { it.describe() }.sorted()}"
    }
    val actualSubjects = actualBySubject.keys
    check(actualSubjects == expected.keys) {
        val missing = (expected.keys - actualSubjects).map { it.describe() }.sorted()
        val unexpected = (actualSubjects - expected.keys).map { it.describe() }.sorted()
        "Feature graph evaluation coverage mismatch; missing: $missing, unexpected: $unexpected"
    }
    evaluation.integrations.forEach { evaluated ->
        check(expected.getValue(evaluated.subject) === evaluated.integration) {
            "Evaluated integration ${evaluated.subject.describe()} does not belong to the supplied feature graph"
        }
    }
}

private fun FeatureIntegrationSubject.describe(): String {
    return "${contentType.value}:${feature.value}:${integration.value}"
}

private data class MissingFixtureCandidate(
    val selection: BehavioralContractSelection,
    val requirement: ContractFixtureDefinition<*>,
)

private data class MissingProjectionCandidate(
    val subject: FeatureIntegrationSubject,
    val requirement: FeatureProjectionDefinition<*>,
)

private data class ProjectionObligationKey(
    val feature: FeatureId,
    val integration: FeatureIntegrationId,
    val requirement: FeatureArtifactId,
)
