package mihon.feature.graph

/** One typed value supplied by its authoritative context owner for a single evaluation snapshot. */
data class ContextEvidence<C : Any>(
    val input: ContextInputDefinition<C>,
    val value: C,
) {
    init {
        require(input.valueType.isInstance(value)) {
            "Context evidence for ${input.id} must be ${input.valueType.qualifiedName}"
        }
    }
}

fun <C : Any> contextEvidence(
    input: ContextInputDefinition<C>,
    value: C,
): ContextEvidence<C> = ContextEvidence(input, value)

/** Complete, immutable evidence exposed only to the owning feature's contextual rule. */
class FeatureContextEvidence internal constructor(
    evidence: List<ContextEvidence<*>>,
) {
    private val evidenceById = evidence.associateBy { it.input.id }

    val inputs: List<ContextInputDefinition<*>> = evidence.map { it.input }

    fun <C : Any> value(input: ContextInputDefinition<C>): C {
        val supplied = requireNotNull(evidenceById[input.id]) {
            "Context rule attempted to read undeclared input ${input.id}"
        }
        require(supplied.input == input) {
            "Context rule requested contradictory definition for ${input.id}"
        }
        @Suppress("UNCHECKED_CAST")
        return supplied.value as C
    }
}

/** Runtime state of one statically discovered conditional integration. */
sealed interface ContextualFeatureIntegrationEvaluation {
    val subject: FeatureIntegrationSubject
    val integration: FeatureIntegration
    val matchedProviders: List<CapabilityProvider<*>>
    val evidence: List<ContextEvidence<*>>
}

@ConsistentCopyVisibility
data class MissingFeatureContextEvidence internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    override val matchedProviders: List<CapabilityProvider<*>>,
    override val evidence: List<ContextEvidence<*>>,
    val missingInputs: List<ContextInputDefinition<*>>,
) : ContextualFeatureIntegrationEvaluation

@ConsistentCopyVisibility
data class BlockedFeatureContext internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    override val matchedProviders: List<CapabilityProvider<*>>,
    override val evidence: List<ContextEvidence<*>>,
    val blockers: List<FeatureContextBlocker>,
) : ContextualFeatureIntegrationEvaluation

@ConsistentCopyVisibility
data class IncompleteFeatureContext internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    override val matchedProviders: List<CapabilityProvider<*>>,
    override val evidence: List<ContextEvidence<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val obligations: List<SpecializedFeatureObligation>,
) : ContextualFeatureIntegrationEvaluation

@ConsistentCopyVisibility
data class ApplicableFeatureContext internal constructor(
    override val subject: FeatureIntegrationSubject,
    override val integration: FeatureIntegration,
    override val matchedProviders: List<CapabilityProvider<*>>,
    override val evidence: List<ContextEvidence<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
) : ContextualFeatureIntegrationEvaluation

/** Runtime resolution and the obligations or descriptive behavior projections activated by exact context evidence. */
@ConsistentCopyVisibility
data class FeatureContextEvaluation internal constructor(
    val integration: ContextualFeatureIntegrationEvaluation,
    val obligations: List<SpecializedFeatureObligation>,
    val behaviorProjections: List<BehaviorProjectionApplicability>,
)

fun resolveFeatureContext(
    evaluation: FeatureGraphEvaluation,
    contentType: ContentTypeId,
    feature: FeatureId,
    integration: FeatureIntegrationId,
    evidence: Iterable<ContextEvidence<*>>,
): FeatureContextEvaluation {
    val matches = evaluation.integrations.filter { evaluated ->
        evaluated.subject.contentType == contentType &&
            evaluated.subject.feature == feature &&
            evaluated.subject.integration == integration
    }
    check(matches.size == 1) {
        "Expected one evaluated integration for ${contentType.value}:${feature.value}:${integration.value}; " +
            "found ${matches.size}"
    }
    val candidate = matches.single() as? ConditionalFeatureIntegration
        ?: error("Integration ${contentType.value}:${feature.value}:${integration.value} is not conditional")
    return resolveFeatureContext(candidate, evidence)
}

internal fun resolveFeatureContext(
    candidate: ConditionalFeatureIntegration,
    evidence: Iterable<ContextEvidence<*>>,
): FeatureContextEvaluation {
    val supplied = evidence.sortedBy { it.input.id.value }
    requireUnique("Context evidence for ${candidate.subject.describe()}", supplied.map { it.input.id.value })

    val declaredById = candidate.unresolvedContextInputs.associateBy { it.id }
    supplied.forEach { item ->
        val declared = requireNotNull(declaredById[item.input.id]) {
            "Unexpected context input ${item.input.id} for ${candidate.subject.describe()}"
        }
        require(declared == item.input) {
            "Contradictory context input ${item.input.id} for ${candidate.subject.describe()}"
        }
    }

    val suppliedIds = supplied.mapTo(mutableSetOf()) { it.input.id }
    val missing = candidate.unresolvedContextInputs.filter { it.id !in suppliedIds }
    if (missing.isNotEmpty()) {
        return FeatureContextEvaluation(
            integration = MissingFeatureContextEvidence(
                subject = candidate.subject,
                integration = candidate.integration,
                matchedProviders = candidate.matchedProviders,
                evidence = supplied,
                missingInputs = missing,
            ),
            obligations = emptyList(),
            behaviorProjections = emptyList(),
        )
    }

    val rule = requireNotNull(candidate.integration.contextRule) {
        "Conditional integration ${candidate.subject.describe()} has no context rule"
    }
    val snapshot = FeatureContextEvidence(supplied)
    return when (val decision = rule.evaluate(snapshot)) {
        FeatureContextDecision.Applicable -> candidate.resolveApplicable(supplied)
        is FeatureContextDecision.Blocked -> candidate.resolveBlocked(supplied, decision.blockers)
    }
}

private fun ConditionalFeatureIntegration.resolveBlocked(
    evidence: List<ContextEvidence<*>>,
    blockers: List<FeatureContextBlocker>,
): FeatureContextEvaluation {
    val declaredBlockers = integration.contextBlockers.associateBy { it.id }
    blockers.forEach { blocker ->
        val declared = requireNotNull(declaredBlockers[blocker.id]) {
            "Context rule returned undeclared blocker ${blocker.id} for ${subject.describe()}"
        }
        require(declared == blocker) {
            "Context rule returned contradictory blocker ${blocker.id} for ${subject.describe()}"
        }
    }
    return FeatureContextEvaluation(
        integration = BlockedFeatureContext(
            subject = subject,
            integration = integration,
            matchedProviders = matchedProviders,
            evidence = evidence,
            blockers = blockers,
        ),
        obligations = emptyList(),
        behaviorProjections = emptyList(),
    )
}

private fun ConditionalFeatureIntegration.resolveApplicable(
    evidence: List<ContextEvidence<*>>,
): FeatureContextEvaluation {
    val adaptersById = suppliedAdaptersById()
    val requirements = integration.specializedRequirements.sortedBy { it.id.value }
    val selectedAdapters = suppliedAdapters.sortedBy { it.definition.id.value }
    val missingRequirements = requirements.filter { it.id !in adaptersById }
    if (missingRequirements.isNotEmpty()) {
        val obligations = missingRequirements.map { requirement ->
            SpecializedFeatureObligation(
                responsibleOwner = subject.contentTypeOwner,
                subject = subject,
                requirement = requirement,
            )
        }
        return FeatureContextEvaluation(
            integration = IncompleteFeatureContext(
                subject = subject,
                integration = integration,
                matchedProviders = matchedProviders,
                evidence = evidence,
                suppliedAdapters = selectedAdapters,
                obligations = obligations,
            ),
            obligations = obligations,
            behaviorProjections = emptyList(),
        )
    }

    return FeatureContextEvaluation(
        integration = ApplicableFeatureContext(
            subject = subject,
            integration = integration,
            matchedProviders = matchedProviders,
            evidence = evidence,
            suppliedAdapters = selectedAdapters,
        ),
        obligations = emptyList(),
        behaviorProjections = integration.behaviorProjections
            .sortedBy { it.id.value }
            .map { projection -> BehaviorProjectionApplicability(subject, projection) },
    )
}

private fun ConditionalFeatureIntegration.suppliedAdaptersById(): Map<SpecializedAdapterId, SpecializedAdapter<*>> {
    return suppliedAdapters.associateBy { it.definition.id }
}

private fun FeatureIntegrationSubject.describe(): String =
    "${contentType.value}:${feature.value}:${integration.value}"
