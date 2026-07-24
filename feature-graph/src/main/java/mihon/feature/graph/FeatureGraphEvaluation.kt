package mihon.feature.graph

/** The stable subject of one feature integration evaluated for one content type. */
data class FeatureIntegrationSubject(
    val contentType: ContentTypeId,
    val contentTypeOwner: ContributionOwner,
    val feature: FeatureId,
    val featureOwner: ContributionOwner,
    val integration: FeatureIntegrationId,
)

/** Result of evaluating a positive capability expression against one content type's providers. */
data class CapabilityExpressionEvaluation(
    val isSatisfied: Boolean,
    val matchedProviders: List<CapabilityProvider<*>>,
    val unmetRequirements: List<CapabilityExpression>,
)

/** Evaluation state of one feature integration for one content type. */
sealed interface FeatureIntegrationEvaluation {
    val subject: FeatureIntegrationSubject
    val integration: FeatureIntegration
}

/** A prerequisite provider is absent. This is ordinary unsupported behavior and creates no obligation. */
@ConsistentCopyVisibility
data class InapplicableFeatureIntegration internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    val matchedProviders: List<CapabilityProvider<*>>,
    val unmetPrerequisites: List<CapabilityExpression>,
    val unmetSpecializedPrerequisites: List<SpecializedAdapterDefinition<*>> = emptyList(),
) : FeatureIntegrationEvaluation

/** Capability prerequisites are satisfied, but runtime context must decide whether the integration applies. */
@ConsistentCopyVisibility
data class ConditionalFeatureIntegration internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    val matchedProviders: List<CapabilityProvider<*>>,
    val unresolvedContextInputs: List<ContextInputDefinition<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val pendingSpecializedRequirements: List<SpecializedAdapterDefinition<*>>,
) : FeatureIntegrationEvaluation

/** Prerequisites are satisfied, but the affected content type has not supplied required specialized work. */
@ConsistentCopyVisibility
data class IncompleteFeatureIntegration internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val obligations: List<SpecializedFeatureObligation>,
) : FeatureIntegrationEvaluation

/** All statically evaluable prerequisites and specialized requirements are satisfied. */
@ConsistentCopyVisibility
data class ApplicableFeatureIntegration internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
) : FeatureIntegrationEvaluation

/**
 * Actionable media-specific work exposed only after the feature's prerequisites are satisfied.
 *
 * The feature owner defines the requirement. The affected content-type owner is responsible for supplying its adapter.
 */
sealed interface FeatureObligation {
    val responsibleOwner: ContributionOwner
}

data class SpecializedFeatureObligation(
    override val responsibleOwner: ContributionOwner,
    val subject: FeatureIntegrationSubject,
    val requirement: SpecializedAdapterDefinition<*>,
) : FeatureObligation

/**
 * An applicability edge from one content type to one feature-owned behavior projection.
 *
 * The projection object is not copied or instantiated per content type. It only describes behavior already owned by
 * the Feature coordinator.
 */
data class BehaviorProjectionApplicability(
    val subject: FeatureIntegrationSubject,
    val projection: FeatureBehaviorProjection,
)

/** A conditional behavior projection discovered statically without granting runtime applicability. */
data class BehaviorProjectionCandidate(
    val subject: FeatureIntegrationSubject,
    val projection: FeatureBehaviorProjection,
)

/** Deterministic derived evaluation of an assembled [FeatureGraph]. */
@ConsistentCopyVisibility
data class FeatureGraphEvaluation internal constructor(
    val integrations: List<FeatureIntegrationEvaluation>,
    val obligations: List<SpecializedFeatureObligation>,
    val behaviorProjections: List<BehaviorProjectionApplicability>,
    val candidateBehaviorProjections: List<BehaviorProjectionCandidate>,
    val executionParticipants: List<FeatureExecutionParticipantEvaluation>,
    val executionObligations: List<SpecializedExecutionParticipantObligation>,
)

fun evaluateFeatureGraph(graph: FeatureGraph): FeatureGraphEvaluation {
    val evaluations = buildList {
        graph.contentTypes.forEach { contentType ->
            graph.features.forEach { feature ->
                feature.integrations.sortedBy { it.id.value }.forEach { integration ->
                    add(evaluateIntegration(contentType, feature, integration))
                }
            }
        }
    }

    val executionParticipants = evaluateFeatureExecutionParticipants(graph)
    return FeatureGraphEvaluation(
        integrations = evaluations,
        obligations = evaluations
            .filterIsInstance<IncompleteFeatureIntegration>()
            .flatMap { it.obligations },
        behaviorProjections = evaluations
            .filterIsInstance<ApplicableFeatureIntegration>()
            .flatMap { evaluation ->
                evaluation.integration.behaviorProjections
                    .sortedBy { it.id.value }
                    .map { projection -> BehaviorProjectionApplicability(evaluation.subject, projection) }
            },
        candidateBehaviorProjections = evaluations
            .filterIsInstance<ConditionalFeatureIntegration>()
            .flatMap { evaluation ->
                evaluation.integration.behaviorProjections
                    .sortedBy { it.id.value }
                    .map { projection -> BehaviorProjectionCandidate(evaluation.subject, projection) }
            },
        executionParticipants = executionParticipants,
        executionObligations = executionParticipants
            .filterIsInstance<IncompleteFeatureExecutionParticipant>()
            .flatMap { it.obligations },
    )
}

fun CapabilityExpression.evaluateAgainst(
    providers: List<CapabilityProvider<*>>,
): CapabilityExpressionEvaluation {
    val providersById = providers.associateBy { it.capability.id }
    return evaluate(providersById)
}

private fun evaluateIntegration(
    contentType: ContentTypeContribution,
    feature: FeatureContribution,
    integration: FeatureIntegration,
): FeatureIntegrationEvaluation {
    val subject = FeatureIntegrationSubject(
        contentType = contentType.contentType,
        contentTypeOwner = contentType.owner,
        feature = feature.feature,
        featureOwner = feature.owner,
        integration = integration.id,
    )
    val prerequisiteResult = integration.prerequisites.evaluateAgainst(contentType.providers)

    val adaptersById = contentType.specializedAdapters.associateBy { it.definition.id }
    val specializedPrerequisites = integration.specializedPrerequisites.sortedBy { it.id.value }
    val missingSpecializedPrerequisites = specializedPrerequisites.filter { it.id !in adaptersById }

    if (!prerequisiteResult.isSatisfied || missingSpecializedPrerequisites.isNotEmpty()) {
        return InapplicableFeatureIntegration(
            subject = subject,
            integration = integration,
            matchedProviders = prerequisiteResult.matchedProviders,
            unmetPrerequisites = prerequisiteResult.unmetRequirements,
            unmetSpecializedPrerequisites = missingSpecializedPrerequisites,
        )
    }

    val requirements = integration.specializedRequirements.sortedBy { it.id.value }
    val suppliedAdapters = (specializedPrerequisites + requirements)
        .distinctBy { it.id }
        .mapNotNull { adaptersById[it.id] }
    val missingRequirements = requirements.filter { it.id !in adaptersById }

    if (integration.contextInputs.isNotEmpty()) {
        return ConditionalFeatureIntegration(
            subject = subject,
            integration = integration,
            matchedProviders = prerequisiteResult.matchedProviders,
            unresolvedContextInputs = integration.contextInputs.sortedBy { it.id.value },
            suppliedAdapters = suppliedAdapters,
            pendingSpecializedRequirements = missingRequirements,
        )
    }

    if (missingRequirements.isNotEmpty()) {
        return IncompleteFeatureIntegration(
            subject = subject,
            integration = integration,
            matchedProviders = prerequisiteResult.matchedProviders,
            suppliedAdapters = suppliedAdapters,
            obligations = missingRequirements.map { requirement ->
                SpecializedFeatureObligation(
                    responsibleOwner = contentType.owner,
                    subject = subject,
                    requirement = requirement,
                )
            },
        )
    }

    return ApplicableFeatureIntegration(
        subject = subject,
        integration = integration,
        matchedProviders = prerequisiteResult.matchedProviders,
        suppliedAdapters = suppliedAdapters,
    )
}

private fun CapabilityExpression.evaluate(
    providersById: Map<CapabilityId, CapabilityProvider<*>>,
): CapabilityExpressionEvaluation {
    return when (this) {
        CapabilityExpression.Always -> satisfiedExpression()

        is CapabilityExpression.Provided -> {
            val provider = providersById[capability.id]
            if (provider == null) {
                unsatisfiedExpression(this)
            } else {
                satisfiedExpression(listOf(provider))
            }
        }

        is CapabilityExpression.AllOf -> {
            val results = terms.map { it.evaluate(providersById) }
            if (results.all { it.isSatisfied }) {
                satisfiedExpression(results.flatMap { it.matchedProviders }.distinctByCapability())
            } else {
                CapabilityExpressionEvaluation(
                    isSatisfied = false,
                    matchedProviders = results.flatMap { it.matchedProviders }.distinctByCapability(),
                    unmetRequirements = results.flatMap { it.unmetRequirements },
                )
            }
        }

        is CapabilityExpression.AnyOf -> {
            val results = terms.map { it.evaluate(providersById) }
            val successfulResults = results.filter { it.isSatisfied }
            if (successfulResults.isEmpty()) {
                val partiallyMatchedProviders = results
                    .flatMap { it.matchedProviders }
                    .distinctByCapability()
                unsatisfiedExpression(this, partiallyMatchedProviders)
            } else {
                satisfiedExpression(successfulResults.flatMap { it.matchedProviders }.distinctByCapability())
            }
        }
    }
}

private fun satisfiedExpression(
    providers: List<CapabilityProvider<*>> = emptyList(),
): CapabilityExpressionEvaluation {
    return CapabilityExpressionEvaluation(
        isSatisfied = true,
        matchedProviders = providers,
        unmetRequirements = emptyList(),
    )
}

private fun unsatisfiedExpression(
    requirement: CapabilityExpression,
    matchedProviders: List<CapabilityProvider<*>> = emptyList(),
): CapabilityExpressionEvaluation {
    return CapabilityExpressionEvaluation(
        isSatisfied = false,
        matchedProviders = matchedProviders,
        unmetRequirements = listOf(requirement),
    )
}

private fun List<CapabilityProvider<*>>.distinctByCapability(): List<CapabilityProvider<*>> {
    return distinctBy { it.capability.id }.sortedBy { it.capability.id.value }
}
