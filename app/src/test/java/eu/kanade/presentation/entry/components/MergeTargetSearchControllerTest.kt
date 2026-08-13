package eu.kanade.presentation.entry.components

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MergeTargetSearchControllerTest {

    @Test
    fun `superseded rank cannot deliver before the latest request`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        try {
            val firstStarted = CountDownLatch(1)
            val releaseFirst = CountDownLatch(1)
            val delivered = CopyOnWriteArrayList<String>()
            val ranked = CopyOnWriteArrayList<String>()
            val controller = MergeTargetSearchController<TestTarget>(
                scope = scope,
                dispatcher = dispatcher,
                ranker = { targets, query ->
                    ranked += query
                    if (query == "first") {
                        firstStarted.countDown()
                        releaseFirst.await(5, TimeUnit.SECONDS)
                    }
                    targets.toList()
                },
            )
            val target = TestTarget("Target")

            controller.submit(listOf(target), "first") { delivered += "first" }
            firstStarted.await(5, TimeUnit.SECONDS) shouldBe true
            controller.submit(listOf(target), "intermediate") { delivered += "intermediate" }
            controller.submit(listOf(target), "latest") { delivered += "latest" }
            releaseFirst.countDown()

            runBlocking {
                withTimeout(5_000) {
                    while (delivered.isEmpty()) kotlinx.coroutines.yield()
                }
            }
            delivered.toList() shouldBe listOf("latest")
            ranked.toList() shouldBe listOf("first", "latest")
        } finally {
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `cancelled pending rank cannot deliver`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        try {
            val rankStarted = CountDownLatch(1)
            val releaseRank = CountDownLatch(1)
            val delivered = CopyOnWriteArrayList<String>()
            val controller = MergeTargetSearchController<TestTarget>(
                scope = scope,
                dispatcher = dispatcher,
                ranker = { targets, _ ->
                    rankStarted.countDown()
                    releaseRank.await(5, TimeUnit.SECONDS)
                    targets.toList()
                },
            )

            controller.submit(listOf(TestTarget("Target")), "query") { delivered += "query" }
            rankStarted.await(5, TimeUnit.SECONDS) shouldBe true
            controller.cancelPending()
            releaseRank.countDown()
            executor.submit {}.get(5, TimeUnit.SECONDS)

            delivered.toList() shouldBe emptyList()
        } finally {
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    private data class TestTarget(
        override val mergeSearchTitle: String,
        override val mergeSearchableTitle: String = mergeSearchTitle,
    ) : MergeSearchTarget
}
