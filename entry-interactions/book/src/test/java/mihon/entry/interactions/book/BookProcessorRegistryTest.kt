package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import mihon.book.api.BookContentDescriptor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BookProcessorRegistryTest {
    private val epub = FakeBookProcessor("epub", "EPUB reader", "application/epub+zip")
    private val alternateEpub = FakeBookProcessor("alternate-epub", "Alternate EPUB reader", "application/epub+zip")
    private val webNovel = FakeBookProcessor("web-novel", "Web novel reader", "text/html")

    @Test
    fun `sole compatible processor is selected automatically`() {
        val registry = BookProcessorRegistry(listOf(epub, webNovel))

        val selection = assertIs<BookProcessorSelection.Selected>(
            registry.select(BookContentDescriptor(format = "application/epub+zip")),
        )

        assertEquals(epub.id, selection.processor.id)
    }

    @Test
    fun `valid remembered processor wins when multiple processors are compatible`() {
        val registry = BookProcessorRegistry(listOf(epub, alternateEpub))

        val selection = assertIs<BookProcessorSelection.Selected>(
            registry.select(
                descriptor = BookContentDescriptor(format = "application/epub+zip"),
                rememberedProcessorId = alternateEpub.id,
            ),
        )

        assertEquals(alternateEpub.id, selection.processor.id)
    }

    @Test
    fun `invalid remembered processor falls back to chooser`() {
        val registry = BookProcessorRegistry(listOf(epub, alternateEpub, webNovel))

        val selection = assertIs<BookProcessorSelection.ChoiceRequired>(
            registry.select(
                descriptor = BookContentDescriptor(format = "application/epub+zip"),
                rememberedProcessorId = webNovel.id,
            ),
        )

        assertEquals(listOf(epub.id, alternateEpub.id), selection.processors.map(BookProcessor::id))
    }

    @Test
    fun `missing compatibility returns unsupported selection`() {
        val registry = BookProcessorRegistry(listOf(epub))

        assertIs<BookProcessorSelection.Unsupported>(
            registry.select(BookContentDescriptor(format = "text/plain")),
        )
    }

    @Test
    fun `duplicate processor IDs are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            BookProcessorRegistry(listOf(epub, FakeBookProcessor(epub.id, "Duplicate", "text/plain")))
        }
    }
}

private class FakeBookProcessor(
    override val id: String,
    override val displayName: String,
    private val format: String,
) : BookProcessor {
    override fun supports(descriptor: BookContentDescriptor): Boolean = descriptor.format == format

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()

    override suspend fun open(content: BookContentSession): BookOpenResult = error("Not used")
}
