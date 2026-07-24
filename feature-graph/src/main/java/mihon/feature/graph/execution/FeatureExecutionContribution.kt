package mihon.feature.graph

import kotlin.reflect.KClass

/** When one coordinator guarantees that contributed work executes relative to its core state change. */
sealed interface FeatureExecutionPhase {
    val id: String

    /** Synchronous work at the caller's current lifecycle position, with no persistence guarantee. */
    data object Inline : FeatureExecutionPhase {
        override val id = "inline"
    }

    /** Work which must execute inside the same host transaction as the core state change. */
    data object InTransaction : FeatureExecutionPhase {
        override val id = "in-transaction"
    }

    /** Process-local work released only after persistence commits successfully. */
    data object AfterCommitVolatile : FeatureExecutionPhase {
        override val id = "after-commit-volatile"
    }

    /** Persisted work which can be delivered after the process-local commit boundary is lost. */
    data object Durable : FeatureExecutionPhase {
        override val id = "durable"
    }
}

/** Whether one participant failure prevents later independent participants from running. */
enum class FeatureExecutionFailurePolicy {
    FAIL_FAST,
    CONTINUE_AND_REPORT,
}

/** A typed executable event boundary owned by the coordinator that emits it. */
sealed interface FeatureExecutionPointDefinition<E : Any> {
    val id: FeatureExecutionPointId
    val owner: ContributionOwner
    val eventType: KClass<E>
    val phase: FeatureExecutionPhase
    val failurePolicy: FeatureExecutionFailurePolicy
}

data class InlineFeatureExecutionPointDefinition<E : Any>(
    override val id: FeatureExecutionPointId,
    override val owner: ContributionOwner,
    override val eventType: KClass<E>,
    override val failurePolicy: FeatureExecutionFailurePolicy,
) : FeatureExecutionPointDefinition<E> {
    override val phase = FeatureExecutionPhase.Inline
}

data class TransactionalFeatureExecutionPointDefinition<E : Any>(
    override val id: FeatureExecutionPointId,
    override val owner: ContributionOwner,
    override val eventType: KClass<E>,
    override val failurePolicy: FeatureExecutionFailurePolicy,
) : FeatureExecutionPointDefinition<E> {
    override val phase = FeatureExecutionPhase.InTransaction
}

data class AfterCommitVolatileFeatureExecutionPointDefinition<E : Any>(
    override val id: FeatureExecutionPointId,
    override val owner: ContributionOwner,
    override val eventType: KClass<E>,
    override val failurePolicy: FeatureExecutionFailurePolicy,
) : FeatureExecutionPointDefinition<E> {
    override val phase = FeatureExecutionPhase.AfterCommitVolatile
}

data class DurableFeatureExecutionPointDefinition<E : Any>(
    override val id: FeatureExecutionPointId,
    override val owner: ContributionOwner,
    override val eventType: KClass<E>,
    override val failurePolicy: FeatureExecutionFailurePolicy,
) : FeatureExecutionPointDefinition<E> {
    override val phase = FeatureExecutionPhase.Durable
}

inline fun <reified E : Any> inlineFeatureExecutionPointDefinition(
    id: FeatureExecutionPointId,
    owner: ContributionOwner,
    failurePolicy: FeatureExecutionFailurePolicy,
): InlineFeatureExecutionPointDefinition<E> = InlineFeatureExecutionPointDefinition(
    id = id,
    owner = owner,
    eventType = E::class,
    failurePolicy = failurePolicy,
)

inline fun <reified E : Any> transactionalFeatureExecutionPointDefinition(
    id: FeatureExecutionPointId,
    owner: ContributionOwner,
    failurePolicy: FeatureExecutionFailurePolicy,
): TransactionalFeatureExecutionPointDefinition<E> = TransactionalFeatureExecutionPointDefinition(
    id = id,
    owner = owner,
    eventType = E::class,
    failurePolicy = failurePolicy,
)

inline fun <reified E : Any> afterCommitVolatileFeatureExecutionPointDefinition(
    id: FeatureExecutionPointId,
    owner: ContributionOwner,
    failurePolicy: FeatureExecutionFailurePolicy,
): AfterCommitVolatileFeatureExecutionPointDefinition<E> = AfterCommitVolatileFeatureExecutionPointDefinition(
    id = id,
    owner = owner,
    eventType = E::class,
    failurePolicy = failurePolicy,
)

inline fun <reified E : Any> durableFeatureExecutionPointDefinition(
    id: FeatureExecutionPointId,
    owner: ContributionOwner,
    failurePolicy: FeatureExecutionFailurePolicy,
): DurableFeatureExecutionPointDefinition<E> = DurableFeatureExecutionPointDefinition(
    id = id,
    owner = owner,
    eventType = E::class,
    failurePolicy = failurePolicy,
)

/** Explicit dependencies between independently owned participants in one execution point. */
data class FeatureExecutionOrder(
    val after: Set<FeatureExecutionParticipantId> = emptySet(),
    val before: Set<FeatureExecutionParticipantId> = emptySet(),
) {
    init {
        require(after.intersect(before).isEmpty()) {
            "An execution participant cannot require the same participant both before and after it"
        }
    }
}

/**
 * One independently owned executable participant.
 *
 * Applicability is evaluated directly from the same content-type providers, contextual evidence, and specialized
 * adapters used by Feature integrations. The target execution point remains owned by its coordinator.
 */
data class FeatureExecutionParticipantDefinition<E : Any>(
    val id: FeatureExecutionParticipantId,
    val owner: ContributionOwner,
    val point: FeatureExecutionPointDefinition<E>,
    val prerequisites: CapabilityExpression = CapabilityExpression.Always,
    val contextInputs: List<ContextInputDefinition<*>> = emptyList(),
    val contextRule: FeatureContextRule? = null,
    val contextBlockers: List<FeatureContextBlocker> = emptyList(),
    val specializedPrerequisites: List<SpecializedAdapterDefinition<*>> = emptyList(),
    val specializedRequirements: List<SpecializedAdapterDefinition<*>> = emptyList(),
    val order: FeatureExecutionOrder = FeatureExecutionOrder(),
    val behavioralContracts: List<FeatureBehaviorContract>,
) {
    init {
        require(id !in order.after && id !in order.before) {
            "Execution participant $id cannot order itself"
        }
        require(contextInputs.isNotEmpty() == (contextRule != null)) {
            "Execution participant $id must declare context inputs and a context rule together"
        }
        contextRule?.let { rule ->
            require(rule.owner == owner) {
                "Execution participant $id cannot use context rule owned by ${rule.owner}"
            }
        }
        requireUnique("Context inputs for execution participant $id", contextInputs.map { it.id.value })
        requireUnique("Context blockers for execution participant $id", contextBlockers.map { it.id.value })
        val declaredContextInputs = contextInputs.toSet()
        contextBlockers.forEach { blocker ->
            blocker.inputs.forEach { input ->
                require(input in declaredContextInputs) {
                    "Context blocker ${blocker.id} on execution participant $id references undeclared input ${input.id}"
                }
            }
        }
        requireUnique(
            "Specialized prerequisites for execution participant $id",
            specializedPrerequisites.map { it.id.value },
        )
        requireUnique(
            "Specialized requirements for execution participant $id",
            specializedRequirements.map { it.id.value },
        )
        require(
            specializedPrerequisites.none { prerequisite ->
                specializedRequirements.any { requirement -> requirement.id == prerequisite.id }
            },
        ) {
            "Execution participant $id cannot use one specialized adapter as both a prerequisite and a requirement"
        }
        (specializedPrerequisites + specializedRequirements).forEach { adapter ->
            require(adapter.owner == owner) {
                "Execution participant $id cannot use specialized adapter ${adapter.id} owned by ${adapter.owner}"
            }
        }
        require(behavioralContracts.isNotEmpty()) {
            "Execution participant $id must declare at least one behavioral contract"
        }
        requireUnique(
            "Behavioral contracts for execution participant $id",
            behavioralContracts.map { it.id.value },
        )
        behavioralContracts
            .flatMap { it.fixtureRequirements }
            .forEach { requirement ->
                require(requirement.owner == owner) {
                    "Execution participant $id cannot own contract fixture ${requirement.id} declared by " +
                        requirement.owner
                }
            }
    }
}
