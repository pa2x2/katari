package eu.kanade.presentation.entry.components

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

internal class MergeTargetSearchController<T : MergeSearchTarget>(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
    private val ranker: (Iterable<T>, String) -> List<T> = ::rankMergeTargets,
) {
    private val generation = AtomicLong()
    private val requests = Channel<Request<T>>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                val result = withContext(dispatcher) {
                    ranker(request.targets, request.query).toImmutableList()
                }
                if (generation.get() == request.generation) {
                    request.onResult(result)
                }
            }
        }
    }

    fun submit(
        targets: List<T>,
        query: String,
        onResult: (ImmutableList<T>) -> Unit,
    ) {
        val request = Request(
            generation = generation.incrementAndGet(),
            targets = targets,
            query = query,
            onResult = onResult,
        )
        requests.trySend(request)
    }

    fun cancelPending() {
        generation.incrementAndGet()
        while (requests.tryReceive().isSuccess) {
            // Drain a queued request while any active non-cooperative rank finishes.
        }
    }

    private data class Request<T>(
        val generation: Long,
        val targets: List<T>,
        val query: String,
        val onResult: (ImmutableList<T>) -> Unit,
    )
}
