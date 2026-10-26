package tachiyomi.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.RejectedExecutionException
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

internal suspend fun AndroidDatabaseHandler.getCurrentDatabaseContext(): CoroutineContext {
    return coroutineContext[TransactionElement]?.transactionDispatcher ?: queryDispatcher
}

internal suspend fun <T> AndroidDatabaseHandler.withTransaction(block: suspend () -> T): T {
    // The driver runs a transaction's block on a thread of its own, not on the one created here, and sees the
    // transaction only from that thread: a statement from any other thread notifies its queries at once, so they re-read
    // before the commit and never after it. A nested transaction runs on the dispatcher of the enclosing block, moving
    // back to it from wherever its caller has switched to
    coroutineContext[EnclosingTransaction]?.let { enclosing ->
        return withContext(enclosing.dispatcher) { db.transactionWithResult { block() } }
    }
    return withContext(createTransactionContext()) {
        val transactionElement = coroutineContext[TransactionElement]!!
        transactionElement.acquire()
        try {
            db.transactionWithResult {
                // Not this scope's coroutineContext, which is the withContext above, on the thread created here
                val dispatcher = currentCoroutineContext()[ContinuationInterceptor]!!
                withContext(EnclosingTransaction(dispatcher)) { block() }
            }
        } finally {
            transactionElement.release()
        }
    }
}

/** The dispatcher an outer transaction's block runs on, where its nested transactions run too. */
private class EnclosingTransaction(val dispatcher: ContinuationInterceptor) :
    AbstractCoroutineContextElement(EnclosingTransaction) {
    companion object Key : CoroutineContext.Key<EnclosingTransaction>
}

private suspend fun AndroidDatabaseHandler.createTransactionContext(): CoroutineContext {
    val controlJob = Job()
    coroutineContext[Job]?.invokeOnCompletion {
        controlJob.cancel()
    }

    val dispatcher = transactionDispatcher.acquireTransactionThread(controlJob)
    val transactionElement = TransactionElement(controlJob, dispatcher)
    val threadLocalElement =
        suspendingTransactionId.asContextElement(System.identityHashCode(controlJob))
    return dispatcher + transactionElement + threadLocalElement
}

private suspend fun CoroutineDispatcher.acquireTransactionThread(
    controlJob: Job,
): ContinuationInterceptor {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            controlJob.cancel()
        }
        try {
            dispatch(EmptyCoroutineContext) {
                runBlocking {
                    continuation.resume(coroutineContext[ContinuationInterceptor]!!)
                    controlJob.join()
                }
            }
        } catch (ex: RejectedExecutionException) {
            continuation.cancel(
                IllegalStateException(
                    "Unable to acquire a thread to perform the database transaction",
                    ex,
                ),
            )
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
private class TransactionElement(
    private val transactionThreadControlJob: Job,
    val transactionDispatcher: ContinuationInterceptor,
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<TransactionElement>

    override val key: CoroutineContext.Key<TransactionElement>
        get() = TransactionElement

    private val referenceCount = AtomicInt(0)

    fun acquire() {
        referenceCount.incrementAndFetch()
    }

    fun release() {
        val count = referenceCount.decrementAndFetch()
        if (count < 0) {
            throw IllegalStateException("Transaction was never started or was already released")
        } else if (count == 0) {
            transactionThreadControlJob.cancel()
        }
    }
}
