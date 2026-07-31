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
    private val alternateModel = BookPublicationModelDescriptor("book.alternate")
    private val documentModel = BookPublicationModelDescriptor("book.document")
    private val alternate = FakeBookProcessor("alternate", "Alternate reader", alternateModel)
    private val secondAlternate = FakeBookProcessor("second-alternate", "Second alternate reader", alternateModel)
    private val documentChapter = FakeBookProcessor("document-chapter", "Document reader", documentModel)

    @Test
    fun `sole compatible processor is selected automatically`() {
        val registry = BookReaderProcessorRegistry(listOf(alternate, documentChapter))

        val selection = assertIs<BookReaderProcessorSelection.Selected>(
            registry.select(alternateModel),
        )

        assertEquals(alternate.id, selection.processor.id)
    }

    @Test
    fun `valid remembered processor wins when multiple processors are compatible`() {
        val registry = BookReaderProcessorRegistry(listOf(alternate, secondAlternate))

        val selection = assertIs<BookReaderProcessorSelection.Selected>(
            registry.select(
                model = alternateModel,
                rememberedProcessorId = secondAlternate.id,
            ),
        )

        assertEquals(secondAlternate.id, selection.processor.id)
    }

    @Test
    fun `invalid remembered processor falls back to chooser`() {
        val registry = BookReaderProcessorRegistry(listOf(alternate, secondAlternate, documentChapter))

        val selection = assertIs<BookReaderProcessorSelection.ChoiceRequired>(
            registry.select(
                model = alternateModel,
                rememberedProcessorId = documentChapter.id,
            ),
        )

        assertEquals(listOf(alternate.id, secondAlternate.id), selection.processors.map(BookReaderProcessor::id))
    }

    @Test
    fun `missing compatibility returns unsupported selection`() {
        val registry = BookReaderProcessorRegistry(listOf(alternate))

        assertIs<BookReaderProcessorSelection.Unsupported>(
            registry.select(BookPublicationModelDescriptor("book.unknown")),
        )
    }

    @Test
    fun `duplicate processor IDs are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            BookReaderProcessorRegistry(listOf(alternate, FakeBookProcessor(alternate.id, "Duplicate", documentModel)))
        }
    }

    @Test
    fun `potential capabilities are associated with processor settings surfaces`() {
        val document = FakeBookProcessor(
            id = "document",
            displayName = "Document",
            model = documentModel,
            settingsSurfaceId = "builtin.book.document",
            capabilities = setOf(StandardReaderCapabilities.StableTextSelection),
        )
        val alternateDocument = FakeBookProcessor(
            id = "alternate-document",
            displayName = "Alternate document",
            model = documentModel,
            settingsSurfaceId = "builtin.book.document",
            capabilities = setOf(StandardReaderCapabilities.SelectionAnchoring),
        )

        val capabilitiesBySurface = BookReaderProcessorRegistry(
            listOf(document, alternateDocument),
        ).potentialReaderCapabilitiesBySettingsSurface()

        assertEquals(
            mapOf(
                "builtin.book.document" to setOf(
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
