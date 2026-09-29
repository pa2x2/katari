package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
internal class SourceBookMaterializationTest : SourceBookContentSessionFixture() {
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
