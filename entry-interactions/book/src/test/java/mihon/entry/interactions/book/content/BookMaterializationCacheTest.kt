package mihon.entry.interactions.book.content

import android.app.Application
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentResource
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookMaterializationCacheTest {
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
    fun `clear keeps leased entries and in-flight atomic writes`() = runTest {
        val cache = cache()
        val leased = cache.acquire(key("active"), metadata()) { it.writeText("active") }
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
        assertTrue(leased.file.exists())
        resume.complete(Unit)
        val written = opening.await()
        assertEquals("complete", written.file.readText())
        leased.close()
        written.close()
        assertEquals(2, cache.clear())
        assertFalse(leased.file.exists())
    }

    private fun cache(
        directory: File = Files.createTempDirectory("katari-book-cache").toFile(),
    ): BookMaterializationCache = BookMaterializationCache(
        application = mockk<Application>(relaxed = true),
        directory = directory,
    )

    private fun key(revision: String): BookMaterializationKey = BookMaterializationKey(
        publicationId = "source:42:entry:/book",
        resourceId = "publication",
        revision = revision,
        mediaType = "text/html",
    )

    private fun metadata(): BookContentResource = BookContentResource(
        id = "publication",
        mediaType = "text/html",
    )
}
