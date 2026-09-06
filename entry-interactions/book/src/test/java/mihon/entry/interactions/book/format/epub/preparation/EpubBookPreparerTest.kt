package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.test.runTest
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.document.BookDocumentTextDirection
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EpubBookPreparerTest {
    @Test
    fun `reflowable package becomes one multi-document canonical publication`() = runTest {
        val file = epubPublicationFile()
        val result = EpubBookPreparer().prepare(EpubContentSessionFixture(file))
        val prepared = assertIs<BookPreparationResult.Success>(result, result.toString()).publication
        try {
            val publication = assertIs<PreparedBookDocumentPublication>(prepared)
            val model = assertIs<BookDocumentPublicationModel>(publication.model)
            assertEquals(listOf("en"), publication.publication.languages)

            assertEquals(
                listOf("OPS/one.xhtml", "OPS/two.xhtml", "OPS/notes.xhtml"),
                model.documents.map { it.resourceId },
            )
            assertEquals(
                listOf("OPS/one.xhtml", "OPS/two.xhtml"),
                publication.publication.readingOrder.map { it.id },
            )
            assertEquals(listOf("One", "Two", "Notes"), publication.publication.navigation.map { it.title })
            val first = model.documents.first()
            assertTrue(first.blocks.any { it.content is BookDocumentBlockContent.Unsupported })
            val styledParagraph = first.blocks.first { it.plainText == "Continue" }
            assertEquals(0.5f, styledParagraph.style.spacingBeforeEm)
            assertEquals(1f, styledParagraph.style.spacingAfterEm)
            assertEquals(1.5f, styledParagraph.style.lineHeightScale)
            assertEquals(2f, styledParagraph.style.firstLineIndentEm)
            assertEquals(BookDocumentTextDirection.RIGHT_TO_LEFT, styledParagraph.style.direction)
            assertEquals(BookDocumentFontFamily.Resource("OPS/font.otf"), styledParagraph.style.fontFamily)
            val links = first.blocks.flatMap { it.links }.map { it.target }
            val link = links.filterIsInstance<BookDocumentLinkTarget.Resource>().single()
            assertEquals(BookDocumentLinkTarget.Resource("OPS/two.xhtml", "there"), link)
            assertEquals(
                BookDocumentLinkTarget.Reference("OPS/notes.xhtml", "n1"),
                links.filterIsInstance<BookDocumentLinkTarget.Reference>().single(),
            )
            val figureBlock = assertNotNull(
                first.blocks.firstOrNull { it.content is BookDocumentBlockContent.Figure },
                first.blocks.joinToString { "${it.role.kind}:${it.plainText}" },
            )
            val figure = assertIs<BookDocumentBlockContent.Figure>(figureBlock.content)
            assertEquals("OPS/cover.png", figure.image.resourceId)
            assertEquals(false, figure.image.decorative)
            val image = publication.resourceLoader.load(
                resourceId = figure.image.resourceId,
                acceptedMediaTypes = setOf("image/png"),
                maxBytes = 1024,
            ).getOrThrow()
            assertTrue(image.bytes.contentEquals(byteArrayOf(1, 2, 3)))
            val inlineVector = first.blocks
                .mapNotNull { block -> (block.content as? BookDocumentBlockContent.Figure)?.image }
                .first { it.resourceId.startsWith("katari-derived/vector/") }
            assertEquals(
                "image/svg+xml",
                publication.resourceLoader.load(
                    resourceId = inlineVector.resourceId,
                    acceptedMediaTypes = setOf("image/svg+xml"),
                    maxBytes = 1024,
                ).getOrThrow().mediaType,
            )
            val font = publication.resourceLoader.load(
                resourceId = "OPS/font.otf",
                acceptedMediaTypes = setOf("font/otf"),
                maxBytes = 1024,
            ).getOrThrow()
            assertTrue(font.bytes.contentEquals("clear font bytes".encodeToByteArray()))
            assertTrue(publication.requiredResourceIds.isEmpty())
        } finally {
            prepared.close()
            file.delete()
        }
    }
}
