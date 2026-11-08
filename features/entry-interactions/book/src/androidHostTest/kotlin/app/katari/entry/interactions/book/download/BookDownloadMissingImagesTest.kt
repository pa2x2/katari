package app.katari.entry.interactions.book.download

import app.katari.entry.interactions.book.content.BookLinkedResourceKind
import app.katari.entry.interactions.book.content.linkedResourceId
import app.katari.entry.interactions.book.download.model.BookDownloadFailure
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.SocketTimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
internal class BookDownloadMissingImagesTest : ProseChapterDownloadFixture() {
    @Test
    fun `a dead image is left out of the download while a timeout fails it for a retry`() = runTest {
        val html = "<p>Opening</p><img src='https://img.example/gone.png' alt='Map'>" +
            "<img src='https://img.example/kept.png' alt='Seal'><img src='never-declared' alt='Crest'>"

        val withDeadImage = downloadProseChapter(html) { chain ->
            if (chain.request().url.encodedPath == "/gone.png") chain.respond(404) else chain.respond(200, pngBytes)
        }
        val withTimeout = downloadProseChapter(html) { chain ->
            if (chain.request().url.encodedPath == "/gone.png") {
                throw SocketTimeoutException("timeout")
            }
            chain.respond(200, pngBytes)
        }

        assertNull(withDeadImage.failure)
        assertEquals(
            listOf("chapter", linkedResourceId("https://img.example/kept.png", BookLinkedResourceKind.IMAGE)),
            assertNotNull(withDeadImage.completedPackage).manifest.resources.map { it.id },
        )
        assertEquals(BookDownloadFailure.Reason.NETWORK, withTimeout.failure?.reason)
        assertNull(withTimeout.completedPackage)
    }
}
