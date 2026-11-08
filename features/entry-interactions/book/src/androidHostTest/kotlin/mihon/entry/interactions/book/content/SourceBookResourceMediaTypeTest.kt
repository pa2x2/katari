package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

internal class SourceBookResourceMediaTypeTest : SourceBookContentSessionFixture() {
    @Test
    fun `an undeclared image served as octet-stream loads on screen and materializes for downloads as its real type`() =
        runTest {
            val webp = "RIFF".encodeToByteArray() + byteArrayOf(26, 0, 0, 0) + "WEBPVP8 ".encodeToByteArray() +
                ByteArray(18)
            val resolver = object : BookExternalResourceResolver {
                override suspend fun open(location: BookResourceLocation, range: BookByteRange?) =
                    object : ExternalBookResource {
                        override val stream = ByteArrayInputStream(webp)
                        override val mediaType = "application/octet-stream"
                        override fun close() = stream.close()
                    }
            }
            val session = session(
                media = bookMedia(
                    resources = listOf(
                        resource("cover", BookResourceLocation.RemoteRequest("https://cdn.example.invalid/i/8f2a")),
                    ),
                ),
                resolver = resolver,
            )

            val loaded = BookContentSessionResourceLoader(session).load(
                resourceId = "cover",
                acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
            ).getOrThrow()
            val materialized = session.materializeResource("cover").getOrThrow()

            assertEquals("image/webp", loaded.mediaType)
            assertEquals(webp.toList(), loaded.bytes.toList())
            assertEquals("image/webp", materialized.use { it.metadata.mediaType })
        }
}
