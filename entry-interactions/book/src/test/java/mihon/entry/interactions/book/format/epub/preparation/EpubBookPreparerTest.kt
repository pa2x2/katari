package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.document.BookDocumentTextDirection
import mihon.entry.interactions.book.content.BookByteRange
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.MaterializedBookResource
import mihon.entry.interactions.book.content.OpenedBookResource
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EpubBookPreparerTest {
    @Test
    fun `reflowable package becomes one multi-document canonical publication`() = runTest {
        val file = publicationFile()
        val result = EpubBookPreparer().prepare(FakeEpubContentSession(file))
        val prepared = assertIs<BookPreparationResult.Success>(result, result.toString()).publication
        try {
            val publication = assertIs<PreparedBookDocumentPublication>(prepared)
            val model = assertIs<BookDocumentPublicationModel>(publication.model)

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

    private fun publicationFile(): File {
        val file = Files.createTempFile("katari-epub", ".epub").toFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            fun entry(path: String, value: ByteArray) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(value)
                zip.closeEntry()
            }
            fun entry(path: String, value: String) = entry(path, value.encodeToByteArray())

            entry("mimetype", EpubContract.FORMAT)
            entry(
                "META-INF/container.xml",
                """<container><rootfiles><rootfile full-path="OPS/package.opf"/></rootfiles></container>""",
            )
            entry(
                "META-INF/encryption.xml",
                """
                <encryption>
                  <EncryptedData>
                    <EncryptionMethod Algorithm="http://www.idpf.org/2008/embedding"/>
                    <CipherData><CipherReference URI="OPS/font.otf"/></CipherData>
                  </EncryptedData>
                </encryption>
                """.trimIndent(),
            )
            entry(
                "OPS/package.opf",
                """
                <package version="3.0" unique-identifier="uid">
                  <metadata><identifier id="uid">urn:uuid:test-publication</identifier><language>en</language></metadata>
                  <manifest>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="one" href="one.xhtml" media-type="application/xhtml+xml"/>
                    <item id="two" href="two.xhtml" media-type="application/xhtml+xml"/>
                    <item id="notes" href="notes.xhtml" media-type="application/xhtml+xml"/>
                    <item id="image" href="cover.png" media-type="image/png"/>
                    <item id="font" href="font.otf" media-type="font/otf"/>
                    <item id="style" href="style.css" media-type="text/css"/>
                  </manifest>
                  <spine><itemref idref="one"/><itemref idref="two"/><itemref idref="notes" linear="no"/></spine>
                </package>
                """.trimIndent(),
            )
            entry(
                "OPS/nav.xhtml",
                """<html><body><nav epub:type="toc"><ol><li><a href="one.xhtml">One</a></li><li><a href="two.xhtml#there">Two</a></li><li><a href="notes.xhtml">Notes</a></li></ol></nav></body></html>""",
            )
            entry(
                "OPS/one.xhtml",
                """
                <html><head><link rel="stylesheet" href="style.css"/></head><body>
                  <p><a href="two.xhtml#there">Continue</a></p>
                  <p><a epub:type="noteref" href="notes.xhtml#n1">Note</a></p>
                  <math><mi>x</mi></math>
                  <div><a id="empty-anchor"/><div><span id="page"/><div><img src="cover.png" alt="Cover"/></div></div></div>
                  <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" aria-label="Square"><rect width="10" height="10"/></svg>
                </body></html>
                """.trimIndent(),
            )
            entry(
                "OPS/two.xhtml",
                """<html><body><p id="there">Destination</p><math><mi>y</mi></math></body></html>""",
            )
            entry("OPS/notes.xhtml", """<html><body><aside><p id="n1">Optional note</p></aside></body></html>""")
            entry("OPS/cover.png", byteArrayOf(1, 2, 3))
            entry(
                "OPS/style.css",
                """
                @font-face { font-family: CustomBookFont; src: url('font.otf'); }
                p { margin-top: 0.5em; margin-bottom: 1em; line-height: 1.5; text-indent: 2em; direction: rtl; font-family: CustomBookFont; }
                """.trimIndent(),
            )
            val clearFont = "clear font bytes".encodeToByteArray()
            val key = MessageDigest.getInstance("SHA-1").digest("urn:uuid:test-publication".encodeToByteArray())
            entry(
                "OPS/font.otf",
                clearFont.copyOf().also { bytes ->
                    bytes.indices.forEach { index ->
                        bytes[index] = (bytes[index].toInt() xor key[index % key.size].toInt()).toByte()
                    }
                },
            )
        }
        return file
    }
}

private class FakeEpubContentSession(
    private val file: File,
) : BookContentSession {
    private val resource = BookContentResource(
        id = "book.epub",
        mediaType = EpubContract.FORMAT,
        size = file.length(),
        revision = "resource-revision",
        availability = BookResourceAvailability.AVAILABLE,
        capabilities = setOf(BookResourceCapability.MATERIALIZE),
    )
    override val descriptor = BookContentDescriptor(EpubContract.FORMAT)
    override val publicationId = "publication"
    override val revision = "publication-revision"
    override val catalogRevision: String? = null
    override val catalogCoverage = BookCatalogCoverage.COMPLETE
    override val resourceHierarchy: List<BookContentResourceGroup> = emptyList()
    override val primaryResourceIds = listOf(resource.id)

    override suspend fun listResources(cursor: String?, limit: Int) =
        Result.success(BookContentResourcePage(listOf(resource)))

    override suspend fun getResource(resourceId: String) = Result.success(resource)

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> =
        error("The EPUB preparer must materialize its primary resource")

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> = Result.success(
        object : MaterializedBookResource {
            override val metadata = resource
            override val file = this@FakeEpubContentSession.file
            override fun close() = Unit
        },
    )

    override fun close() = Unit
}
