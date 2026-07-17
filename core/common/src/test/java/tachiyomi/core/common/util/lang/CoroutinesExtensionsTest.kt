package tachiyomi.core.common.util.lang

import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test

class CoroutinesExtensionsTest {

    @Test
    fun `non-cancellable launch survives immediate owner cancellation`() {
        runBlocking {
            val owner = Job()
            val scope = CoroutineScope(owner + Dispatchers.Default)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()

            val job = scope.launchNonCancellable {
                started.complete(Unit)
                release.await()
                completed.complete(Unit)
            }
            scope.cancel()

            withTimeout(5_000) { started.await() }
            release.complete(Unit)
            job.join()

            started.isCompleted.shouldBeTrue()
            completed.isCompleted.shouldBeTrue()
        }
    }
}
