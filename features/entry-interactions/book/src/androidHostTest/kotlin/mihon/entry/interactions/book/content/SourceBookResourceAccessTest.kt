package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.EntryMedia
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
internal class SourceBookResourceAccessTest : SourceBookContentSessionFixture() {
    @Test
    fun `source child loops and mismatched media fail without recursion`() = runTest {
        val loopingSource = source()
        coEvery { loopingSource.getMedia(any(), any()) } returns EntryMedia.Book(
            descriptor = BookContentDescriptor("text/html"),
            initialResourceId = "chapter-1",
            initialResourceLocation = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
        )
        val loopSession = session(
            source = loopingSource,
            media = bookMedia(
                resources = listOf(
                    resource(
                        "chapter-1",
                        location = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
                    ),
                ),
            ),
        )

        val loopFailure = assertNotNull(loopSession.openResource("chapter-1").exceptionOrNull())
        assertTrue(loopFailure.message.orEmpty().contains("loop"))

        val mismatchedSource = source()
        coEvery { mismatchedSource.getMedia(any(), any()) } returns EntryMedia.ImagePages(emptyList())
        val mismatchSession = session(
            source = mismatchedSource,
            media = bookMedia(
                resources = listOf(
                    resource(
                        "chapter-1",
                        location = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
                    ),
                ),
            ),
        )

        val mismatchFailure = assertNotNull(mismatchSession.openResource("chapter-1").exceptionOrNull())
        assertTrue(mismatchFailure.message.orEmpty().contains("non-BOOK"))
    }
}
