package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
internal class SourceBookMaterializationTest : SourceBookContentSessionFixture() {
    @Test
    fun `versioned materializations are cached across leases and remain clearable`() = runTest {
        val directory = Files.createTempDirectory("katari-book-session-test").toFile()
        val cache = BookMaterializationCache(application(), directory)
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        "chapter",
                        mediaType = "text/html",
                        location = BookResourceLocation.InlineBytes("chapter-content".encodeToByteArray()),
                    ),
                ),
                initialResourceId = "chapter",
                initialLocation = BookResourceLocation.InlineBytes("chapter-content".encodeToByteArray()),
            ),
            materializationStore = cache,
        )

        val materialized = session.materializeResource("chapter").getOrThrow()
        assertTrue(materialized.file.exists())
        assertTrue(materialized.file.name.endsWith(".html"))
        assertEquals("chapter-content", materialized.file.readText())

        materialized.close()
        assertTrue(materialized.file.exists())

        val outstanding = session.materializeResource("chapter").getOrThrow()
        assertEquals(materialized.file, outstanding.file)
        session.close()
        assertTrue(outstanding.file.exists())
        assertEquals(1, cache.clear())
        assertFalse(outstanding.file.exists())
        assertTrue(session.getResource("chapter").isFailure)
    }

    @Test
    fun `declared oversized resource fails before external access`() = runTest {
        val resolver = FakeExternalResolver(emptyMap())
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        id = "huge",
                        size = 512L * 1024L * 1024L + 1L,
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/huge"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        assertTrue(session.materializeResource("huge").isFailure)
        assertTrue(resolver.requests.isEmpty())
    }

    @Test
    fun `bounded materialization stops reading after the acquisition limit`() = runTest {
        val bytesRead = AtomicInteger()
        val resolver = object : BookExternalResourceResolver {
            override suspend fun open(
                location: BookResourceLocation,
                range: BookByteRange?,
            ): ExternalBookResource {
                val stream = object : InputStream() {
                    private var position = 0

                    override fun read(): Int {
                        if (position == 64) return -1
                        position++
                        bytesRead.incrementAndGet()
                        return 1
                    }

                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        if (position == 64) return -1
                        val count = minOf(length, 64 - position)
                        buffer.fill(1, offset, offset + count)
                        position += count
                        bytesRead.addAndGet(count)
                        return count
                    }
                }
                return object : ExternalBookResource {
                    override val stream: InputStream = stream
                    override fun close() = stream.close()
                }
            }
        }
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        id = "unknown-size",
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/unknown-size"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        val failure = session.materializeResource("unknown-size", maxBytes = 4).exceptionOrNull()

        assertIs<BookResourceMaterializationLimitException>(failure)
        assertEquals(5, bytesRead.get())
    }

    @Test
    fun `session close releases outstanding streams once`() = runTest {
        val resolver = FakeExternalResolver(
            mapOf("remote:https://example.invalid/book" to "content".encodeToByteArray()),
        )
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        "remote",
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        session.openResource("remote").getOrThrow()
        session.close()
        session.close()

        assertEquals(1, resolver.closeCount.get())
    }

    @Test
    fun `cancellation propagates across the session result boundary`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        "remote",
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                    ),
                ),
            ),
            resolver = object : BookExternalResourceResolver {
                override suspend fun open(
                    location: BookResourceLocation,
                    range: BookByteRange?,
                ): ExternalBookResource = throw CancellationException("cancelled")
            },
        )

        assertFailsWith<CancellationException> { session.openResource("remote") }
    }
}
