package mihon.feature.graph

import java.util.concurrent.CancellationException

fun interface FeatureExecutionHandler<E : Any> {
    suspend fun execute(event: E)
}

fun interface FeatureExecutionContextResolver<E : Any> {
    suspend fun resolve(event: E): List<ContextEvidence<*>>
}

/** Runtime implementation supplied by the same owner as its participant declaration. */
data class FeatureExecutionParticipantBinding<E : Any>(
    val definition: FeatureExecutionParticipantDefinition<E>,
    val handler: FeatureExecutionHandler<E>,
    val contextResolver: FeatureExecutionContextResolver<E>? = null,
) {
    init {
        require(definition.point.phase != FeatureExecutionPhase.Durable) {
            "Durable execution participant ${definition.id} requires a durable runtime binding"
        }
        require(definition.contextInputs.isNotEmpty() == (contextResolver != null)) {
            "Execution participant ${definition.id} must bind a context resolver exactly when it declares context inputs"
        }
    }
}

data class FeatureExecutionFailure(
    val participant: FeatureExecutionParticipantId,
    val owner: ContributionOwner,
    val error: Throwable,
)

data class FeatureExecutionResult(
    val point: FeatureExecutionPointId,
    val contentType: ContentTypeId,
    val selectedParticipants: List<FeatureExecutionParticipantId>,
    val completedParticipants: List<FeatureExecutionParticipantId>,
    val failures: List<FeatureExecutionFailure>,
    val stoppedEarly: Boolean,
) {
    val isSuccessful: Boolean
        get() = failures.isEmpty()
}

/** Executes the applicable runtime bindings selected by one evaluated Feature Graph. */
class FeatureExecutionRuntime(
    private val graph: FeatureGraph,
    private val evaluation: FeatureGraphEvaluation,
    bindings: List<FeatureExecutionParticipantBinding<*>>,
    durableBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
) {
    private val bindingsById: Map<FeatureExecutionParticipantId, FeatureExecutionParticipantBinding<*>>
    private val durableRuntime = FeatureDurableExecutionRuntime(graph, evaluation, durableBindings)

    init {
        validateEvaluationCoverage()
        val duplicateBindings = bindings.groupBy { it.definition.id }.filterValues { it.size > 1 }
        check(duplicateBindings.isEmpty()) {
            "Duplicate execution participant bindings: ${duplicateBindings.keys.sortedBy { it.value }}"
        }
        val declaredById = graph.executionParticipants.associateBy { it.id }
        val transientDeclaredById = declaredById.filterValues { it.point.phase != FeatureExecutionPhase.Durable }
        bindingsById = bindings.associateBy { it.definition.id }

        val missing = transientDeclaredById.keys - bindingsById.keys
        val orphaned = bindingsById.keys - transientDeclaredById.keys
        check(missing.isEmpty() && orphaned.isEmpty()) {
            "Execution participant binding coverage mismatch; missing: ${missing.sortedBy { it.value }}, " +
                "orphaned: ${orphaned.sortedBy { it.value }}"
        }
        bindingsById.forEach { (id, binding) ->
            check(binding.definition == transientDeclaredById.getValue(id)) {
                "Execution participant binding $id does not match its graph declaration"
            }
        }
    }

    suspend fun <E : Any> executeInline(
        point: InlineFeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureExecutionResult = executeTransient(point, contentType, event)

    internal suspend fun <E : Any> executeTransactional(
        point: TransactionalFeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureExecutionResult = executeTransient(point, contentType, event)

    internal suspend fun <E : Any> executeAfterCommitVolatile(
        point: AfterCommitVolatileFeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureExecutionResult = executeTransient(point, contentType, event)

    private suspend fun <E : Any> executeTransient(
        point: FeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureExecutionResult {
        check(point.phase != FeatureExecutionPhase.Durable) {
            "Durable execution point ${point.id} must be prepared and delivered through the durable runtime API"
        }
        val declaredPoint = graph.executionPoints.singleOrNull { it.id == point.id }
            ?: error("Unknown execution point ${point.id}")
        check(declaredPoint == point) {
            "Execution point ${point.id} does not match its graph declaration"
        }
        require(point.eventType.isInstance(event)) {
            "Execution point ${point.id} requires event ${point.eventType.qualifiedName}, " +
                "received ${event::class.qualifiedName}"
        }
        check(graph.contentTypes.any { it.contentType == contentType }) {
            "Unknown execution content type $contentType"
        }

        val applicableIds = mutableSetOf<FeatureExecutionParticipantId>()
        evaluation.executionParticipants
            .filter { evaluated ->
                evaluated.subject.contentType == contentType && evaluated.subject.point == point.id
            }
            .forEach { evaluated ->
                when (evaluated) {
                    is ApplicableFeatureExecutionParticipant -> applicableIds += evaluated.subject.participant
                    is InapplicableFeatureExecutionParticipant -> Unit
                    is IncompleteFeatureExecutionParticipant -> error(
                        "Execution participant ${evaluated.subject.participant} is incomplete for $contentType: " +
                            evaluated.obligations.map { it.requirement.id },
                    )
                    is ConditionalFeatureExecutionParticipant -> {
                        val binding = bindingsById.getValue(evaluated.subject.participant)
                        val evidence = resolveContext(binding, event)
                        when (val resolved = resolveFeatureExecutionContext(evaluated, evidence)) {
                            is ApplicableFeatureExecutionContext -> applicableIds += evaluated.subject.participant
                            is BlockedFeatureExecutionContext -> Unit
                            is IncompleteFeatureExecutionContext -> error(
                                "Execution participant ${evaluated.subject.participant} is contextually incomplete " +
                                    "for $contentType: ${resolved.obligations.map { it.requirement.id }}",
                            )
                            is MissingFeatureExecutionContextEvidence -> error(
                                "Execution participant ${evaluated.subject.participant} is missing context evidence: " +
                                    resolved.missingInputs.map { it.id },
                            )
                        }
                    }
                }
            }

        val selected = orderedExecutionParticipants(point, graph.executionParticipants)
            .filter { it.id in applicableIds }
        val completed = mutableListOf<FeatureExecutionParticipantId>()
        val failures = mutableListOf<FeatureExecutionFailure>()
        var stoppedEarly = false
        for (participant in selected) {
            val binding = bindingsById.getValue(participant.id)
            try {
                executeBinding(binding, event)
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
        return FeatureExecutionResult(
            point = point.id,
            contentType = contentType,
            selectedParticipants = selected.map { it.id },
            completedParticipants = completed,
            failures = failures,
            stoppedEarly = stoppedEarly,
        )
    }

    suspend fun <E : Any> prepareDurable(
        point: DurableFeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureDurableExecutionPreparationResult = durableRuntime.prepare(point, contentType, event)

    suspend fun deliverDurable(envelope: FeatureDurableExecutionEnvelope) {
        durableRuntime.deliver(envelope)
    }

    suspend fun discardDurable(
        envelopes: List<FeatureDurableExecutionEnvelope>,
    ): List<FeatureExecutionFailure> = durableRuntime.discard(envelopes)

    private fun validateEvaluationCoverage() {
        val expected = buildMap {
            graph.contentTypes.forEach { contentType ->
                graph.executionParticipants.forEach { participant ->
                    put(
                        FeatureExecutionParticipantSubject(
                            contentType = contentType.contentType,
                            contentTypeOwner = contentType.owner,
                            point = participant.point.id,
                            participant = participant.id,
                            participantOwner = participant.owner,
                        ),
                        participant,
                    )
                }
            }
        }
        val actual = evaluation.executionParticipants.groupBy { it.subject }
        check(actual.values.none { it.size > 1 }) {
            "Feature execution evaluation contains duplicate subjects"
        }
        check(actual.keys == expected.keys) {
            val missing = expected.keys - actual.keys
            val unexpected = actual.keys - expected.keys
            "Feature execution evaluation coverage mismatch; missing: $missing, unexpected: $unexpected"
        }
        evaluation.executionParticipants.forEach { evaluated ->
            check(evaluated.participant === expected.getValue(evaluated.subject)) {
                "Evaluated execution participant ${evaluated.subject.participant} does not belong to the graph"
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private suspend fun <E : Any> resolveContext(
    binding: FeatureExecutionParticipantBinding<*>,
    event: E,
): List<ContextEvidence<*>> {
    return (binding.contextResolver as FeatureExecutionContextResolver<E>).resolve(event)
}

@Suppress("UNCHECKED_CAST")
private suspend fun <E : Any> executeBinding(
    binding: FeatureExecutionParticipantBinding<*>,
    event: E,
) {
    (binding.handler as FeatureExecutionHandler<E>).execute(event)
}
