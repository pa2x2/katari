package mihon.feature.graph

import java.util.concurrent.atomic.AtomicBoolean

/**
 * The only runtime surface which accepts execution points that must share a host transaction.
 *
 * Instances are created by [coordinateFeatureCommit] and cannot be manufactured by a coordinator.
 */
class FeatureTransactionalExecutionScope private constructor(
    private val runtime: FeatureExecutionRuntime,
) {
    suspend fun <E : Any> execute(
        point: TransactionalFeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureExecutionResult = runtime.executeTransactional(point, contentType, event)

    internal companion object {
        fun create(runtime: FeatureExecutionRuntime) = FeatureTransactionalExecutionScope(runtime)
    }
}

/**
 * Creates host callbacks without exposing the transactional execution surface to the surrounding coordinator.
 *
 * The host receives the returned callback and owns the exact point at which it is invoked inside its transaction.
 */
class FeatureTransactionCallbackFactory internal constructor(
    private val runtime: FeatureExecutionRuntime,
) {
    private val active = AtomicBoolean(true)

    fun callback(
        block: suspend FeatureTransactionalExecutionScope.() -> Unit,
    ): suspend () -> Unit {
        val invoked = AtomicBoolean(false)
        return {
            requireInvocation(invoked)
            FeatureTransactionalExecutionScope.create(runtime).block()
        }
    }

    fun <T> callback(
        block: suspend FeatureTransactionalExecutionScope.(T) -> Unit,
    ): suspend (T) -> Unit {
        val invoked = AtomicBoolean(false)
        return { value ->
            requireInvocation(invoked)
            FeatureTransactionalExecutionScope.create(runtime).block(value)
        }
    }

    internal fun close() {
        active.set(false)
    }

    private fun requireInvocation(invoked: AtomicBoolean) {
        check(active.get()) { "Feature transaction callback escaped its host commit boundary" }
        check(invoked.compareAndSet(false, true)) { "Feature transaction callback can execute only once" }
    }
}

/**
 * The only runtime surface which accepts volatile post-commit execution points.
 *
 * Instances are released after a successful [coordinateFeatureCommit], or by a boundary which has already received a
 * successful persistence result and explicitly enters [afterFeatureCommitVolatile].
 */
class FeatureAfterCommitVolatileExecutionScope internal constructor(
    private val runtime: FeatureExecutionRuntime,
) {
    suspend fun <E : Any> execute(
        point: AfterCommitVolatileFeatureExecutionPointDefinition<E>,
        contentType: ContentTypeId,
        event: E,
    ): FeatureExecutionResult = runtime.executeAfterCommitVolatile(point, contentType, event)
}

sealed interface FeatureCommitExecutionResult<out C, out A> {
    val commit: C

    data class NotCommitted<C>(
        override val commit: C,
    ) : FeatureCommitExecutionResult<C, Nothing>

    data class Committed<C, A>(
        override val commit: C,
        val volatileConsequences: A,
    ) : FeatureCommitExecutionResult<C, A>
}

/**
 * Coordinates the two execution surfaces around a host-owned persistence boundary.
 *
 * The host remains responsible for invoking callbacks inside its actual transaction. The coordinator receives only a
 * callback factory, not a transactional execution scope, and volatile consequences are released only when [committed]
 * accepts the host result.
 */
suspend fun <C, A> FeatureExecutionRuntime.coordinateFeatureCommit(
    commit: suspend FeatureTransactionCallbackFactory.() -> C,
    committed: (C) -> Boolean,
    volatileConsequences: suspend FeatureAfterCommitVolatileExecutionScope.(C) -> A,
): FeatureCommitExecutionResult<C, A> {
    val callbacks = FeatureTransactionCallbackFactory(this)
    val commitResult = try {
        callbacks.commit()
    } finally {
        callbacks.close()
    }
    if (!committed(commitResult)) {
        return FeatureCommitExecutionResult.NotCommitted(commitResult)
    }
    val consequences = FeatureAfterCommitVolatileExecutionScope(this).volatileConsequences(commitResult)
    return FeatureCommitExecutionResult.Committed(commitResult, consequences)
}

/**
 * Enters a volatile post-commit boundary when persistence is owned by a lower layer which has already returned success.
 */
suspend fun <A> FeatureExecutionRuntime.afterFeatureCommitVolatile(
    block: suspend FeatureAfterCommitVolatileExecutionScope.() -> A,
): A = FeatureAfterCommitVolatileExecutionScope(this).block()
