package mihon.text.recognition.runtime.execution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.runtime.cache.TextRecognitionCacheKey
import mihon.text.recognition.runtime.cache.TextRecognitionResultCache

/**
 * Serves results from the cache and otherwise runs recognition one request at a time.
 *
 * Model inference saturates the CPU and holds large buffers, so concurrent runs would only compete. Requests queue
 * fairly in arrival order, and a request that waited behind an identical one is answered from the cache it filled.
 */
internal class CachedRecognitionExecutor(
    private val cache: TextRecognitionResultCache,
    private val ioDispatcher: CoroutineDispatcher,
    private val inferenceDispatcher: CoroutineDispatcher,
) {
    private val inference = Mutex()

    suspend fun execute(
        key: TextRecognitionCacheKey,
        recognize: suspend () -> List<RecognizedTextRegion>,
    ): List<RecognizedTextRegion> {
        cached(key)?.let { return it }
        return inference.withLock {
            cached(key) ?: withContext(inferenceDispatcher) { recognize() }.also { regions ->
                withContext(ioDispatcher) { cache.write(key, regions) }
            }
        }
    }

    private suspend fun cached(key: TextRecognitionCacheKey): List<RecognizedTextRegion>? =
        withContext(ioDispatcher) { cache.read(key) }
}
