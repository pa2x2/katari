package mihon.feature.graph.execution

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.ContextInputDefinition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContextEvidence
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureSubjectContribution
import mihon.feature.graph.FeatureSubjectReference
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.SpecializedAdapterDefinition
import mihon.feature.graph.SpecializedExecutionParticipantObligation
import mihon.feature.graph.evaluateAgainst
import mihon.feature.graph.reference
import mihon.feature.graph.requireUnique

/** One participant evaluated for one installed Feature subject. */
data class FeatureExecutionParticipantSubject(
    val affectedSubject: FeatureSubjectReference,
    val point: FeatureExecutionPointId,
    val participant: FeatureExecutionParticipantId,
    val participantOwner: ContributionOwner,
)

sealed interface FeatureExecutionParticipantEvaluation {
    val subject: FeatureExecutionParticipantSubject
    val participant: FeatureExecutionParticipantDefinition<*>
}

@ConsistentCopyVisibility
data class InapplicableFeatureExecutionParticipant internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    val matchedProviders: List<CapabilityProvider<*>>,
    val unmetPrerequisites: List<CapabilityExpression>,
    val unmetSpecializedPrerequisites: List<SpecializedAdapterDefinition<*>> = emptyList(),
) : FeatureExecutionParticipantEvaluation

@ConsistentCopyVisibility
data class ConditionalFeatureExecutionParticipant internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    val matchedProviders: List<CapabilityProvider<*>>,
    val unresolvedContextInputs: List<ContextInputDefinition<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val pendingSpecializedRequirements: List<SpecializedAdapterDefinition<*>>,
) : FeatureExecutionParticipantEvaluation

@ConsistentCopyVisibility
data class IncompleteFeatureExecutionParticipant internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val obligations: List<SpecializedExecutionParticipantObligation>,
) : FeatureExecutionParticipantEvaluation

@ConsistentCopyVisibility
data class ApplicableFeatureExecutionParticipant internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
) : FeatureExecutionParticipantEvaluation

internal fun evaluateFeatureExecutionParticipants(
    graph: FeatureGraph,
): List<FeatureExecutionParticipantEvaluation> {
    return buildList {
        graph.subjects.forEach { subject ->
            graph.executionParticipants
                .filter { it.point.subjectScope == subject.subject.scope }
                .forEach { participant ->
                    add(evaluateFeatureExecutionParticipant(subject, participant))
                }
        }
    }
}

private fun evaluateFeatureExecutionParticipant(
    affectedSubject: FeatureSubjectContribution,
    participant: FeatureExecutionParticipantDefinition<*>,
): FeatureExecutionParticipantEvaluation {
    val subject = FeatureExecutionParticipantSubject(
        affectedSubject = affectedSubject.reference(),
        point = participant.point.id,
        participant = participant.id,
        participantOwner = participant.owner,
    )
    val prerequisiteResult = participant.prerequisites.evaluateAgainst(affectedSubject.providers)
    val adaptersById = affectedSubject.specializedAdapters.associateBy { it.definition.id }
    val specializedPrerequisites = participant.specializedPrerequisites.sortedBy { it.id.value }
    val missingSpecializedPrerequisites = specializedPrerequisites.filter { it.id !in adaptersById }

    if (!prerequisiteResult.isSatisfied || missingSpecializedPrerequisites.isNotEmpty()) {
        return InapplicableFeatureExecutionParticipant(
            subject = subject,
            participant = participant,
            matchedProviders = prerequisiteResult.matchedProviders,
            unmetPrerequisites = prerequisiteResult.unmetRequirements,
            unmetSpecializedPrerequisites = missingSpecializedPrerequisites,
        )
    }

    val requirements = participant.specializedRequirements.sortedBy { it.id.value }
    val suppliedAdapters = (specializedPrerequisites + requirements)
        .distinctBy { it.id }
        .mapNotNull { adaptersById[it.id] }
    val missingRequirements = requirements.filter { it.id !in adaptersById }

    if (participant.contextInputs.isNotEmpty()) {
        return ConditionalFeatureExecutionParticipant(
            subject = subject,
            participant = participant,
            matchedProviders = prerequisiteResult.matchedProviders,
            unresolvedContextInputs = participant.contextInputs.sortedBy { it.id.value },
            suppliedAdapters = suppliedAdapters,
            pendingSpecializedRequirements = missingRequirements,
        )
    }

    if (missingRequirements.isNotEmpty()) {
        return IncompleteFeatureExecutionParticipant(
            subject = subject,
            participant = participant,
            matchedProviders = prerequisiteResult.matchedProviders,
            suppliedAdapters = suppliedAdapters,
            obligations = missingRequirements.map { requirement ->
                SpecializedExecutionParticipantObligation(
                    responsibleOwner = affectedSubject.owner,
                    subject = subject,
                    requirement = requirement,
                )
            },
        )
    }

    return ApplicableFeatureExecutionParticipant(
        subject = subject,
        participant = participant,
        matchedProviders = prerequisiteResult.matchedProviders,
        suppliedAdapters = suppliedAdapters,
    )
}

sealed interface ContextualFeatureExecutionParticipantEvaluation {
    val subject: FeatureExecutionParticipantSubject
    val participant: FeatureExecutionParticipantDefinition<*>
    val evidence: List<ContextEvidence<*>>
}

@ConsistentCopyVisibility
data class MissingFeatureExecutionContextEvidence internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    override val evidence: List<ContextEvidence<*>>,
    val missingInputs: List<ContextInputDefinition<*>>,
) : ContextualFeatureExecutionParticipantEvaluation

@ConsistentCopyVisibility
data class BlockedFeatureExecutionContext internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    override val evidence: List<ContextEvidence<*>>,
    val blockers: List<FeatureContextBlocker>,
) : ContextualFeatureExecutionParticipantEvaluation

@ConsistentCopyVisibility
data class IncompleteFeatureExecutionContext internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    override val evidence: List<ContextEvidence<*>>,
    val obligations: List<SpecializedExecutionParticipantObligation>,
) : ContextualFeatureExecutionParticipantEvaluation

@ConsistentCopyVisibility
data class ApplicableFeatureExecutionContext internal constructor(
    override val subject: FeatureExecutionParticipantSubject,
    override val participant: FeatureExecutionParticipantDefinition<*>,
    override val evidence: List<ContextEvidence<*>>,
) : ContextualFeatureExecutionParticipantEvaluation

fun resolveFeatureExecutionContext(
    candidate: ConditionalFeatureExecutionParticipant,
    evidence: Iterable<ContextEvidence<*>>,
): ContextualFeatureExecutionParticipantEvaluation {
    val supplied = evidence.sortedBy { it.input.id.value }
    requireUnique(
        "Context evidence for execution participant ${candidate.subject.participant}",
        supplied.map { it.input.id.value },
    )
    val declaredById = candidate.unresolvedContextInputs.associateBy { it.id }
    supplied.forEach { item ->
        val declared = requireNotNull(declaredById[item.input.id]) {
            "Unexpected context input ${item.input.id} for execution participant ${candidate.subject.participant}"
        }
        require(declared == item.input) {
            "Contradictory context input ${item.input.id} for execution participant ${candidate.subject.participant}"
        }
    }
    val suppliedIds = supplied.mapTo(mutableSetOf()) { it.input.id }
    val missing = candidate.unresolvedContextInputs.filter { it.id !in suppliedIds }
    if (missing.isNotEmpty()) {
        return MissingFeatureExecutionContextEvidence(
            subject = candidate.subject,
            participant = candidate.participant,
            evidence = supplied,
            missingInputs = missing,
        )
    }

    val rule = requireNotNull(candidate.participant.contextRule)
    return when (val decision = rule.evaluate(FeatureContextEvidence(supplied))) {
        is FeatureContextDecision.Blocked -> {
            val declaredBlockers = candidate.participant.contextBlockers.associateBy { it.id }
            decision.blockers.forEach { blocker ->
                val declared = requireNotNull(declaredBlockers[blocker.id]) {
                    "Execution participant ${candidate.subject.participant} returned undeclared blocker ${blocker.id}"
                }
                require(declared == blocker) {
                    "Execution participant ${candidate.subject.participant} returned contradictory blocker ${blocker.id}"
                }
            }
            BlockedFeatureExecutionContext(
                subject = candidate.subject,
                participant = candidate.participant,
                evidence = supplied,
                blockers = decision.blockers,
            )
        }
        FeatureContextDecision.Applicable -> {
            val suppliedAdapterIds = candidate.suppliedAdapters.mapTo(mutableSetOf()) { it.definition.id }
            val missingRequirements = candidate.pendingSpecializedRequirements.filter { it.id !in suppliedAdapterIds }
            if (missingRequirements.isNotEmpty()) {
                IncompleteFeatureExecutionContext(
                    subject = candidate.subject,
                    participant = candidate.participant,
                    evidence = supplied,
                    obligations = missingRequirements.map { requirement ->
                        SpecializedExecutionParticipantObligation(
                            responsibleOwner = candidate.subject.affectedSubject.owner,
                            subject = candidate.subject,
                            requirement = requirement,
                        )
                    },
                )
            } else {
                ApplicableFeatureExecutionContext(
                    subject = candidate.subject,
                    participant = candidate.participant,
                    evidence = supplied,
                )
            }
        }
    }
}
