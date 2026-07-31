package mihon.entry.interactions.book.prose

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.BookResource
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.PreparedBookPublication
import mihon.entry.interactions.book.TestBookPublicationResourceLoader
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

internal class HtmlProseChapterProjectorTest : HtmlProseDocumentFixture() {
    @Test
    fun `accepts the canonical model without depending on its preparer wrapper`() = runTest {
        val document = prepare("<p>First paragraph</p><p>Second paragraph</p>").document
        val publication = AlternateDocumentPublication(document)

        val projection = HtmlProseChapterProjector().project(
            owner = chapter(),
            publication = publication,
            locator = BookLocator(document.resourceId, progression = 0.5),
        )

        assertNotNull(projection)
        assertEquals(document, projection.chapter.document.document)
        assertSame(TestBookPublicationResourceLoader, projection.chapter.resourceLoader)
    }

    @Test
    fun `reuses only rendered document while applying the latest reading position`() = runTest {
        val document = prepare("<p>First paragraph</p><p>Second paragraph</p><p>Third paragraph</p>").document
        val publication = AlternateDocumentPublication(document)
        val projector = HtmlProseChapterProjector()
        val original = assertNotNull(
            projector.project(
                owner = chapter(),
                publication = publication,
                locator = BookLocator(document.resourceId, progression = 0.0),
            ),
        )

        val restored = assertNotNull(
            projector.project(
                owner = chapter(),
                publication = publication,
                locator = BookLocator(document.resourceId, progression = 0.9),
                reusableDocument = original.chapter.document,
            ),
        )

        assertSame(original.chapter.document, restored.chapter.document)
        assertNotEquals(original.chapter.initialPosition, restored.chapter.initialPosition)
        assertEquals(document.positionAtProgression(0.9f), restored.chapter.initialPosition)
    }

    @Test
    fun `dispatches new Android document projection to the configured worker`() = runTest {
        val document = prepare("<p>Projected prose</p>").document
        val dispatcher = RecordingDispatcher()

        assertNotNull(
            HtmlProseChapterProjector(dispatcher).project(
                owner = chapter(),
                publication = AlternateDocumentPublication(document),
                locator = null,
            ),
        )

        assertEquals(1, dispatcher.dispatchCount.get())
    }
}

private class AlternateDocumentPublication(
    document: mihon.book.api.document.BookDocument,
) : PreparedBookPublication {
    override val model = BookDocumentPublicationModel(listOf(document))
    override val resourceLoader = TestBookPublicationResourceLoader
    override val publication = BookPublication(
        id = "alternate-document-publication",
        revision = "1",
        title = "Alternate Document Publication",
        languages = emptyList(),
        readingDirection = null,
        readingOrder = listOf(BookResource(document.resourceId, "text/html", "Chapter")),
        navigation = emptyList(),
    )

    override fun validate(locator: BookLocator): Boolean =
        locator.resourceId == model.documents.single().resourceId &&
            locator.progression?.let { it.isFinite() && it in 0.0..1.0 } != false

    override fun close() = Unit
}

private class RecordingDispatcher : CoroutineDispatcher() {
    val dispatchCount = AtomicInteger()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount.incrementAndGet()
        block.run()
    }
}
