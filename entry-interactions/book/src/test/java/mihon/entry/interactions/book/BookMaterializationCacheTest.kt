package mihon.entry.interactions.book

import android.app.Application
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentResource
import mihon.book.api.BookResourceCacheState
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BookMaterializationCacheTest {
    @Test
    fun `stable revision reuses one atomic materialization`() = runTest {
        val cache = cache()
        val writes = AtomicInteger()

        val first = cache.acquire(key(revision = "v1"), metadata()) { file ->
            writes.incrementAndGet()
            file.writeText("publication")
        }
        first.close()
        val second = cache.acquire(key(revision = "v1"), metadata()) {
            error("cached materialization should be reused")
        }

        assertEquals(1, writes.get())
        assertEquals(first.file, second.file)
        assertEquals("publication", second.file.readText())
        assertEquals(BookResourceCacheState.CACHED, cache.cacheState(key(revision = "v1")))
        second.close()
    }

    @Test
    fun `revision change creates a different cache entry`() = runTest {
        val cache = cache()
        val first = cache.acquire(key("v1"), metadata()) { it.writeText("one") }
        first.close()
        val second = cache.acquire(key("v2"), metadata()) { it.writeText("two") }

        assertNotEquals(first.file, second.file)
        assertEquals("one", first.file.readText())
        assertEquals("two", second.file.readText())
        second.close()
    }

    @Test
    fun `invalidated stable revision is materialized again`() = runTest {
        val cache = cache()
        val writes = AtomicInteger()
        val stableKey = key("stable")
        val first = cache.acquire(stableKey, metadata()) {
            writes.incrementAndGet()
            it.writeText("invalid")
        }

        first.invalidate()
        first.close()
        val second = cache.acquire(stableKey, metadata()) {
            writes.incrementAndGet()
            it.writeText("valid")
        }

        assertEquals(2, writes.get())
        assertEquals("valid", second.file.readText())
        second.close()
    }

    @Test
    fun `unversioned materialization is deleted with its final lease`() = runTest {
        val cache = cache()
        val lease = cache.acquire(null, metadata()) { it.writeText("temporary") }

        assertTrue(lease.file.exists())
        lease.close()
        assertFalse(lease.file.exists())
    }

    @Test
    fun `concurrent opens coalesce on one cache write`() = runTest {
        val cache = cache()
        val writes = AtomicInteger()

        val leases = List(4) {
            async {
                cache.acquire(key("same"), metadata()) { file ->
                    writes.incrementAndGet()
                    delay(10)
                    file.writeText("shared")
                }
            }
        }.awaitAll()

        assertEquals(1, writes.get())
        assertEquals(1, leases.map { it.file }.distinct().size)
        leases.forEach(AutoCloseable::close)
    }

    @Test
    fun `failed and cancelled writes leave no partial files`() = runTest {
        val directory = Files.createTempDirectory("katari-book-cache-failure").toFile()
        val cache = cache(directory)

        assertFailsWith<IllegalStateException> {
            cache.acquire(key("failed"), metadata()) { file ->
                file.writeText("partial")
                error("failed")
            }
        }
        assertTrue(directory.listFiles().orEmpty().isEmpty())

        assertFailsWith<CancellationException> {
            cache.acquire(key("cancelled"), metadata()) { file ->
                file.writeText("partial")
                throw CancellationException("cancelled")
            }
        }
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `clear skips active leases and removes released entries`() = runTest {
        val cache = cache()
        val lease = cache.acquire(key("active"), metadata()) { it.writeText("active") }

        assertEquals(0, cache.clear())
        assertTrue(lease.file.exists())
        lease.close()
        assertEquals(1, cache.clear())
        assertFalse(lease.file.exists())
    }

    @Test
    fun `clear does not remove an in-flight atomic write`() = runTest {
        val directory = Files.createTempDirectory("katari-book-cache-write").toFile()
        val cache = cache(directory)
        val started = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()
        val opening = async {
            cache.acquire(key("writing"), metadata()) { file ->
                file.writeText("partial")
                started.complete(Unit)
                resume.await()
                file.writeText("complete")
            }
        }

        started.await()
        assertEquals(0, cache.clear())
        resume.complete(Unit)
        val lease = opening.await()
        assertEquals("complete", lease.file.readText())
        lease.close()
    }

    @Test
    fun `least recently used released entries are pruned to the cache budget`() = runTest {
        val directory = Files.createTempDirectory("katari-book-cache-prune").toFile()
        val cache = BookMaterializationCache(
            application = mockk<Application>(relaxed = true),
            directory = directory,
            maxCacheBytes = 7,
        )
        val first = cache.acquire(key("first"), metadata()) { it.writeText("1111") }
        first.close()
        val second = cache.acquire(key("second"), metadata()) { it.writeText("2222") }

        assertFalse(first.file.exists())
        assertTrue(second.file.exists())
        second.close()
    }

    private fun cache(
        directory: java.io.File = Files.createTempDirectory("katari-book-cache").toFile(),
    ): BookMaterializationCache = BookMaterializationCache(
        application = mockk<Application>(relaxed = true),
        directory = directory,
    )

    private fun key(revision: String): BookMaterializationKey = BookMaterializationKey(
        publicationId = "source:42:entry:/book",
        resourceId = "publication",
        revision = revision,
        mediaType = "application/epub+zip",
    )

    private fun metadata(): BookContentResource = BookContentResource(
        id = "publication",
        mediaType = "application/epub+zip",
    )
}
