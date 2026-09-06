package mihon.entry.interactions.book.download

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentResource
import mihon.entry.interactions.book.content.MaterializedBookResource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [BookDownloadDocumentsQueryShadow::class])
internal class BookDownloadDocumentStorageTest {
    @Test
    fun `package remains readable when document storage changes the requested resource filename`() = runTest {
        val root = BookDownloadDocumentsProviderFixture().downloadsDirectory()
        val provider = BookDownloadProvider(
            downloadsDirectory = { root },
            directoryListing = BookDownloadDirectoryListing(RuntimeEnvironment.getApplication()),
        )
        val fixture = fixture()
        val staging = provider.beginPackage("Fixture", fixture.entry, fixture.child).getOrThrow()
        val bytes = byteArrayOf(1, 2, 3, 4)
        val resource = object : MaterializedBookResource {
            override val metadata = BookContentResource(id = "chapter", mediaType = "application/octet-stream")
            override val file = Files.createTempFile("book-resource", ".bin").toFile().apply { writeBytes(bytes) }
            override fun close() {
                file.delete()
            }
        }
        resource.use {
            val stored = BookDownloadResourceWriter(
                provider,
                staging,
                BookDownloadResourceBudget().tracker(),
                emptyMap(),
            )
                .write("chapter", resource)
            assertNotEquals(provider.resourceFileName("chapter", resource.metadata.mediaType), stored.fileName)
            val manifest = fixture.manifest(storedSize = stored.storedSize, sha256 = stored.sha256)
                .copy(resources = listOf(stored))

            val completed = provider.completePackage(staging, manifest).getOrThrow()
            val reopened = checkNotNull(provider.readVerifiedPackage(completed.directory))

            assertEquals(stored.fileName, reopened.resources.getValue("chapter").name)
            assertEquals(
                bytes.toList(),
                reopened.resources.getValue("chapter").openInputStream().use {
                    it.readBytes().toList()
                },
            )
        }
    }
}
