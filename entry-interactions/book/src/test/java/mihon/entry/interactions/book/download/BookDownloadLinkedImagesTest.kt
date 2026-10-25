package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.source.entry.BookLinkedResources
import kotlinx.coroutines.test.runTest
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.format.html.prosechapter.preparation.HtmlProseChapterPreparer
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDownloadLinkedImagesTest : ProseChapterDownloadFixture() {
    @Test
    fun `images named by URL are fetched with the source's headers and found again when read offline`() = runTest {
        val requests = Collections.synchronizedList(mutableListOf<Pair<String, String?>>())
        val dataImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes)
        val result = downloadProseChapter(
            html = "<p>Opening</p><img src='img/map.png' alt='Map'><img src='cover' alt='Cover'>" +
                "<img src='$dataImage' alt='Seal'>",
            linkedResources = BookLinkedResources(
                baseUrl = "https://novel.example/book/1/",
                headers = mapOf("Referer" to "https://novel.example/"),
            ),
            catalogImages = mapOf("cover" to pngBytes),
            network = { chain ->
                requests += chain.request().url.toString() to chain.request().header("Referer")
                chain.respond(200, pngBytes)
            },
        )

        assertNull(result.failure)
        assertEquals(listOf("https://novel.example/book/1/img/map.png" to "https://novel.example/"), requests)
        val downloaded = assertNotNull(result.completedPackage)
        // Pinned: chapters downloaded before an update store their images under this ID.
        assertTrue(
            downloaded.manifest.resources.any {
                it.id == "linked-image:f59423e3eb5ae854cc11de302fffc04ddaf6a0127a1ed2472e04609158d0c41d"
            },
        )

        val offline = DownloadedBookContentSession(downloaded, materializationStore)
        val prepared = assertIs<BookPreparationResult.Success>(HtmlProseChapterPreparer().prepare(offline))
        val publication = assertIs<PreparedBookDocumentPublication>(prepared.publication)
        val images = publication.documents.single().blocks.mapNotNull { block ->
            (block.content as? BookDocumentBlockContent.Figure)?.image?.resourceId
        }
        assertEquals(3, images.size)
        images.forEach { resourceId ->
            val loaded = publication.resourceLoader.load(
                resourceId = resourceId,
                acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
            ).getOrThrow()
            assertEquals("image/png", loaded.mediaType)
        }
    }
}
