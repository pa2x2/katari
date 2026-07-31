package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.viewer.settings.ReaderCapabilityId
import mihon.entry.viewer.settings.StandardReaderCapabilities
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BookReaderProcessorRegistryTest {
    private val epubModel = BookPublicationModelDescriptor("book.epub")
    private val proseModel = BookPublicationModelDescriptor("book.document")
    private val epub = FakeBookProcessor("epub", "EPUB reader", epubModel)
    private val alternateEpub = FakeBookProcessor("alternate-epub", "Alternate EPUB reader", epubModel)
    private val proseChapter = FakeBookProcessor("prose-chapter", "Prose chapter reader", proseModel)

    @Test
    fun `sole compatible processor is selected automatically`() {
        val registry = BookReaderProcessorRegistry(listOf(epub, proseChapter))

        val selection = assertIs<BookReaderProcessorSelection.Selected>(
            registry.select(epubModel),
        )

        assertEquals(epub.id, selection.processor.id)
    }

    @Test
    fun `valid remembered processor wins when multiple processors are compatible`() {
        val registry = BookReaderProcessorRegistry(listOf(epub, alternateEpub))

        val selection = assertIs<BookReaderProcessorSelection.Selected>(
            registry.select(
                model = epubModel,
                rememberedProcessorId = alternateEpub.id,
            ),
        )

        assertEquals(alternateEpub.id, selection.processor.id)
    }

    @Test
    fun `invalid remembered processor falls back to chooser`() {
        val registry = BookReaderProcessorRegistry(listOf(epub, alternateEpub, proseChapter))

        val selection = assertIs<BookReaderProcessorSelection.ChoiceRequired>(
            registry.select(
                model = epubModel,
                rememberedProcessorId = proseChapter.id,
            ),
        )

        assertEquals(listOf(epub.id, alternateEpub.id), selection.processors.map(BookReaderProcessor::id))
    }

    @Test
    fun `missing compatibility returns unsupported selection`() {
        val registry = BookReaderProcessorRegistry(listOf(epub))

        assertIs<BookReaderProcessorSelection.Unsupported>(
            registry.select(BookPublicationModelDescriptor("book.unknown")),
        )
    }

    @Test
    fun `duplicate processor IDs are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            BookReaderProcessorRegistry(listOf(epub, FakeBookProcessor(epub.id, "Duplicate", proseModel)))
        }
    }

    @Test
    fun `potential capabilities are associated with processor settings surfaces`() {
        val prose = FakeBookProcessor(
            id = "prose",
            displayName = "Prose",
            model = proseModel,
            settingsSurfaceId = "builtin.book.prose",
            capabilities = setOf(StandardReaderCapabilities.StableTextSelection),
        )
        val alternateProse = FakeBookProcessor(
            id = "alternate-prose",
            displayName = "Alternate prose",
            model = proseModel,
            settingsSurfaceId = "builtin.book.prose",
            capabilities = setOf(StandardReaderCapabilities.SelectionAnchoring),
        )

        val capabilitiesBySurface = BookReaderProcessorRegistry(
            listOf(prose, alternateProse),
        ).potentialReaderCapabilitiesBySettingsSurface()

        assertEquals(
            mapOf(
                "builtin.book.prose" to setOf(
                    StandardReaderCapabilities.StableTextSelection,
                    StandardReaderCapabilities.SelectionAnchoring,
                ),
            ),
            capabilitiesBySurface,
        )
    }
}

private class FakeBookProcessor(
    override val id: String,
    override val displayName: String,
    private val model: BookPublicationModelDescriptor,
    private val settingsSurfaceId: String? = null,
    private val capabilities: Set<ReaderCapabilityId> = emptySet(),
) : BookReaderProcessor {
    override val viewerSettingsSurfaceId = settingsSurfaceId
    override val potentialReaderCapabilities = capabilities

    override fun supports(model: BookPublicationModelDescriptor): Boolean = model == this.model

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()
}
