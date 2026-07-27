package mihon.feature.graph

import java.util.concurrent.CancellationException

fun interface FeatureDurableExecutionPreparer<E : Any> {
    suspend fun prepare(event: E): FeatureDurableExecutionPayload?
}

fun interface FeatureDurableExecutionDeliveryHandler {
    suspend fun deliver(payload: FeatureDurableExecutionPayload)
}

fun interface FeatureDurableExecutionDiscardHandler {
    suspend fun discard(payload: FeatureDurableExecutionPayload)
}

/** Participant-owned opaque state that can be persisted independently of the workflow that requested it. */
data class FeatureDurableExecutionPayload(
    val schemaVersion: Int,
    val value: String,
) {
    init {
        require(schemaVersion > 0) { "Durable execution payload schema version must be positive" }
    }
}

/** Stable persisted routing identity plus an opaque participant-owned payload. */
data class FeatureDurableExecutionEnvelope(
    val participant: FeatureExecutionParticipantId,
    val schemaVersion: Int,
    val payload: String,
) {
    init {
        require(schemaVersion > 0) { "Durable execution envelope schema version must be positive" }
    }

    internal fun toPayload(): FeatureDurableExecutionPayload = FeatureDurableExecutionPayload(schemaVersion, payload)
}

/** Runtime preparation, delivery, and rollback behavior supplied by the participant owner. */
data class FeatureDurableExecutionParticipantBinding<E : Any>(
    val definition: FeatureExecutionParticipantDefinition<E>,
    val preparer: FeatureDurableExecutionPreparer<E>,
    val deliveryHandler: FeatureDurableExecutionDeliveryHandler,
    val discardHandler: FeatureDurableExecutionDiscardHandler = FeatureDurableExecutionDiscardHandler { },
    val contextResolver: FeatureExecutionContextResolver<E>? = null,
) {
    init {
        require(definition.point.phase == FeatureExecutionPhase.Durable) {
            "Non-durable execution participant ${definition.id} requires a transient runtime binding"
        }
        require(definition.contextInputs.isNotEmpty() == (contextResolver != null)) {
            "Durable execution participant ${definition.id} must bind a context resolver exactly when it declares " +
                "context inputs"
        }
    }
}

data class FeatureDurableExecutionPreparationResult(
    val execution: FeatureExecutionResult,
    val envelopes: List<FeatureDurableExecutionEnvelope>,
) {
    val isSuccessful: Boolean
        get() = execution.isSuccessful
}

internal class FeatureDurableExecutionRuntime(
    private val graph: FeatureGraph,
    private val evaluation: FeatureGraphEvaluation,
    bindings: List<FeatureDurableExecutionParticipantBinding<*>>,
) {
    private val bindingsById: Map<FeatureExecutionParticipantId, FeatureDurableExecutionParticipantBinding<*>>

    init {
        val duplicateBindings = bindings.groupBy { it.definition.id }.filterValues { it.size > 1 }
        check(duplicateBindings.isEmpty()) {
            "Duplicate durable execution participant bindings: ${duplicateBindings.keys.sortedBy { it.value }}"
        }
        bindingsById = bindings.associateBy { it.definition.id }
        val declaredById = graph.executionParticipants
            .filter { it.point.phase == FeatureExecutionPhase.Durable }
            .associateBy { it.id }
        val missing = declaredById.keys - bindingsById.keys
        val orphaned = bindingsById.keys - declaredById.keys
        check(missing.isEmpty() && orphaned.isEmpty()) {
            "Durable execution participant binding coverage mismatch; " +
                "missing: ${missing.sortedBy { it.value }}, orphaned: ${orphaned.sortedBy { it.value }}"
        }
        bindingsById.forEach { (id, binding) ->
            check(binding.definition == declaredById.getValue(id)) {
                "Durable execution participant binding $id does not match its graph declaration"
            }
        }
    }

    suspend fun <E : Any> prepare(
        point: DurableFeatureExecutionPointDefinition<E>,
        subject: FeatureSubjectId,
        event: E,
    ): FeatureDurableExecutionPreparationResult {
        validateRequest(point, subject, event)
        val applicableIds = applicableParticipantIds(point, subject, event)
        val selected = orderedExecutionParticipants(point, graph.executionParticipants)
            .filter { it.id in applicableIds }
        val completed = mutableListOf<FeatureExecutionParticipantId>()
        val failures = mutableListOf<FeatureExecutionFailure>()
        val envelopes = mutableListOf<FeatureDurableExecutionEnvelope>()
        var stoppedEarly = false
        for (participant in selected) {
            val binding = bindingsById.getValue(participant.id)
            try {
                prepareBinding(binding, event)?.let { payload ->
                    envelopes += FeatureDurableExecutionEnvelope(
                        participant = participant.id,
                        schemaVersion = payload.schemaVersion,
                        payload = payload.value,
                    )
                }
                completed += participant.id
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures += FeatureExecutionFailure(participant.id, participant.owner, error)
                if (point.failurePolicy == FeatureExecutionFailurePolicy.FAIL_FAST) {
                    stoppedEarly = true
                    break
                }
            }
        }
        return FeatureDurableExecutionPreparationResult(
            execution = FeatureExecutionResult(
                point = point.id,
                affectedSubject = subject,
                selectedParticipants = selected.map { it.id },
                completedParticipants = completed,
                failures = failures,
                stoppedEarly = stoppedEarly,
            ),
            envelopes = envelopes,
        )
    }

    suspend fun deliver(envelope: FeatureDurableExecutionEnvelope) {
        val binding = requireNotNull(bindingsById[envelope.participant]) {
            "No durable execution participant binding for ${envelope.participant}"
        }
        binding.deliveryHandler.deliver(envelope.toPayload())
    }

    suspend fun discard(
        envelopes: List<FeatureDurableExecutionEnvelope>,
    ): List<FeatureExecutionFailure> = buildList {
        envelopes.asReversed().forEach { envelope ->
            val binding = bindingsById[envelope.participant]
            if (binding == null) {
                add(
                    FeatureExecutionFailure(
                        participant = envelope.participant,
                        owner = ContributionOwner("unknown-durable-participant"),
                        error = IllegalStateException(
                            "No durable execution participant binding for ${envelope.participant}",
                        ),
                    ),
                )
                return@forEach
            }
            try {
                binding.discardHandler.discard(envelope.toPayload())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                add(FeatureExecutionFailure(binding.definition.id, binding.definition.owner, error))
            }
        }
    }

    private fun <E : Any> validateRequest(
        point: DurableFeatureExecutionPointDefinition<E>,
        subject: FeatureSubjectId,
        event: E,
    ) {
        val declaredPoint = graph.executionPoints.singleOrNull { it.id == point.id }
            ?: error("Unknown execution point ${point.id}")
        check(declaredPoint == point) {
            "Execution point ${point.id} does not match its graph declaration"
        }
        check(point.subjectScope == subject.scope) {
            "Execution point ${point.id} targets ${point.subjectScope}, not ${subject.scope}"
        }
        require(point.eventType.isInstance(event)) {
            "Execution point ${point.id} requires event ${point.eventType.qualifiedName}, " +
                "received ${event::class.qualifiedName}"
        }
        check(graph.subjects.any { it.subject == subject }) {
            "Unknown execution subject ${subject.stableValue}"
        }
    }

    private suspend fun <E : Any> applicableParticipantIds(
        point: FeatureExecutionPointDefinition<E>,
        subject: FeatureSubjectId,
        event: E,
    ): Set<FeatureExecutionParticipantId> = buildSet {
        evaluation.executionParticipants
            .filter { evaluated ->
                evaluated.subject.affectedSubject.id == subject && evaluated.subject.point == point.id
            }
            .forEach { evaluated ->
                when (evaluated) {
                    is ApplicableFeatureExecutionParticipant -> add(evaluated.subject.participant)
                    is InapplicableFeatureExecutionParticipant -> Unit
                    is IncompleteFeatureExecutionParticipant -> error(
                        "Execution participant ${evaluated.subject.participant} is incomplete for " +
                            "${subject.stableValue}: " +
                            evaluated.obligations.map { it.requirement.id },
                    )
                    is ConditionalFeatureExecutionParticipant -> {
                        val binding = bindingsById.getValue(evaluated.subject.participant)
                        val evidence = resolveContext(binding, event)
                        when (val resolved = resolveFeatureExecutionContext(evaluated, evidence)) {
                            is ApplicableFeatureExecutionContext -> add(evaluated.subject.participant)
                            is BlockedFeatureExecutionContext -> Unit
                            is IncompleteFeatureExecutionContext -> error(
                                "Execution participant ${evaluated.subject.participant} is contextually incomplete " +
                                    "for ${subject.stableValue}: " +
                                    resolved.obligations.map { it.requirement.id },
                            )
                            is MissingFeatureExecutionContextEvidence -> error(
                                "Execution participant ${evaluated.subject.participant} is missing context evidence: " +
                                    resolved.missingInputs.map { it.id },
                            )
                        }
                    }
                }
            }
    }
}

@Suppress("UNCHECKED_CAST")
private suspend fun <E : Any> resolveContext(
    binding: FeatureDurableExecutionParticipantBinding<*>,
    event: E,
): List<ContextEvidence<*>> {
    return (binding.contextResolver as FeatureExecutionContextResolver<E>).resolve(event)
}

@Suppress("UNCHECKED_CAST")
private suspend fun <E : Any> prepareBinding(
    binding: FeatureDurableExecutionParticipantBinding<*>,
    event: E,
): FeatureDurableExecutionPayload? {
    return (binding.preparer as FeatureDurableExecutionPreparer<E>).prepare(event)
}
