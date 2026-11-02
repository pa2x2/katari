package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CloudflareChallengeCoordinatorTest {

    @Test
    fun `same zone callers solve serially`() {
        val executor = Executors.newFixedThreadPool(2)
        val solveStarted = CountDownLatch(1)
        val releaseSolve = CountDownLatch(1)
        val secondRegistered = CountDownLatch(1)
        val secondSolveStarted = CountDownLatch(1)
        val solveCount = AtomicInteger()
        val coordinator = CloudflareChallengeCoordinator(
            zoneFor = { "example.com" },
            onRegistered = { url ->
                if (url.host == "b.example.com") secondRegistered.countDown()
            },
        )

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    solveStarted.countDown()
                    assertTrue(releaseSolve.await(5, TimeUnit.SECONDS))
                }
            }
            assertTrue(solveStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    secondSolveStarted.countDown()
                }
            }
            assertTrue(secondRegistered.await(5, TimeUnit.SECONDS))
            assertFalse(secondSolveStarted.await(100, TimeUnit.MILLISECONDS))
            releaseSolve.countDown()

            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)
            assertEquals(2, solveCount.get())
        } finally {
            releaseSolve.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `same zone waiter can solve after the first caller fails`() {
        val executor = Executors.newFixedThreadPool(2)
        val solveStarted = CountDownLatch(1)
        val releaseSolve = CountDownLatch(1)
        val secondRegistered = CountDownLatch(1)
        val failure = IllegalStateException("solve failed")
        val solveCount = AtomicInteger()
        val coordinator = CloudflareChallengeCoordinator(
            zoneFor = { "example.com" },
            onRegistered = { url ->
                if (url.host == "b.example.com") secondRegistered.countDown()
            },
        )

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    solveStarted.countDown()
                    assertTrue(releaseSolve.await(5, TimeUnit.SECONDS))
                    throw failure
                }
            }
            assertTrue(solveStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                }
            }
            assertTrue(secondRegistered.await(5, TimeUnit.SECONDS))
            releaseSolve.countDown()

            val firstFailure = assertThrows(ExecutionException::class.java) {
                first.get(5, TimeUnit.SECONDS)
            }
            assertEquals(failure, firstFailure.cause)
            second.get(5, TimeUnit.SECONDS)
            assertEquals(2, solveCount.get())
        } finally {
            releaseSolve.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `interrupted solve lets a registered waiter take over`() {
        val executor = Executors.newFixedThreadPool(2)
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondRegistered = CountDownLatch(1)
        val solveCount = AtomicInteger()
        val coordinator = CloudflareChallengeCoordinator(
            zoneFor = { "example.com" },
            onRegistered = { url ->
                if (url.host == "b.example.com") secondRegistered.countDown()
            },
        )

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    firstStarted.countDown()
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS))
                    throw InterruptedException("solve interrupted")
                }
            }
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    assertFalse(Thread.currentThread().isInterrupted)
                    solveCount.incrementAndGet()
                }
            }

            assertTrue(secondRegistered.await(5, TimeUnit.SECONDS))
            releaseFirst.countDown()

            val firstFailure = assertThrows(ExecutionException::class.java) {
                first.get(5, TimeUnit.SECONDS)
            }
            assertTrue(firstFailure.cause is InterruptedException)
            second.get(5, TimeUnit.SECONDS)
            assertEquals(2, solveCount.get())
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }
}
