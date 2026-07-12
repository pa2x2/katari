package mihon.entry.interactions.book.epub

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookTextContext
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.MaterializedBookResource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ReadiumEpubProcessorTest {

    @Test
    fun `opens authored EPUB 2 and maps reading order and nested navigation`() = runBlocking {
        val fixture = EpubFixture.write(temporaryDirectory().resolve("epub2.epub"), version = 2)
        val content = TestContentSession(fixture, publicationId = "book:epub2", revision = "v1")

        val result = ReadiumEpubProcessor().open(content)

        val session = assertIs<BookOpenResult.Success>(result, result.toString()).session
        assertEquals("book:epub2", session.publication.id)
        assertEquals("v1", session.publication.revision)
        assertEquals(2, session.publication.readingOrder.size)
        assertEquals("Part One", session.publication.navigation.single().title)
        assertEquals("Chapter One", session.publication.navigation.single().children.single().title)
        assertFalse(content.closed)
        assertEquals(0, content.leaseCloseCount.get())

        session.close()
        session.close()
        assertEquals(1, content.leaseCloseCount.get())
    }

    @Test
    fun `opens authored EPUB 3 and preserves RTL and anchored navigation`() = runBlocking {
        val fixture = EpubFixture.write(temporaryDirectory().resolve("epub3.epub"), version = 3, rtl = true)
        val content = TestContentSession(fixture, publicationId = "book:epub3", revision = "v2")

        val result = ReadiumEpubProcessor().open(content)
        val session = assertIs<BookOpenResult.Success>(result, result.toString()).session as ReadiumPublicationSession
        val publication = session.publication

        assertEquals(BookReadingDirection.RIGHT_TO_LEFT, publication.readingDirection)
        assertEquals(listOf("ar"), publication.languages)
        val chapterTarget = publication.navigation.single().children.single().target
        assertTrue(chapterTarget.resourceId.endsWith("chapter1.xhtml"))
        assertEquals(listOf("intro"), chapterTarget.fragments)
        assertTrue(publication.readingOrder.any { it.mediaType == "application/xhtml+xml" })

        val locator = BookLocator(
            resourceId = publication.readingOrder.first().id,
            progression = 0.4,
            totalProgression = 0.2,
            logicalPosition = 3,
            fragments = listOf("intro"),
            textContext = BookTextContext(
                before = "before",
                highlight = "highlight",
                after = "after",
            ),
            extensions = mapOf("org.readium.locations" to JsonPrimitive("extension")),
        )
        val readiumLocator = checkNotNull(ReadiumLocatorAdapter.restore(locator, session.readiumPublication()))
        val restored = ReadiumLocatorAdapter.adapt(readiumLocator)
        assertEquals(locator.copy(extensions = emptyMap()), restored.copy(extensions = emptyMap()))
        assertTrue(session.validate(locator))

        session.close()
    }

    @Test
    fun `reports malformed content and releases materialized lease`() = runBlocking {
        val malformed = temporaryDirectory().resolve("malformed.epub").toFile().apply {
            writeText("not an epub")
        }
        val content = TestContentSession(malformed, publicationId = "book:bad", revision = "v1")

        val result = assertIs<BookOpenResult.Failure>(ReadiumEpubProcessor().open(content))

        assertEquals(BookFailureReason.MALFORMED_CONTENT, result.failure.reason)
        assertEquals(1, content.leaseCloseCount.get())
        assertFalse(content.closed)
    }

    private fun temporaryDirectory(): Path = Files.createTempDirectory("katari-readium-spike")
}

private class TestContentSession(
    private val publicationFile: File,
    override val publicationId: String,
    override val revision: String,
) : BookContentSession {
    override val descriptor = BookContentDescriptor(format = "application/epub+zip")
    val leaseCloseCount = AtomicInteger()
    var closed = false
        private set

    override suspend fun materializePrimaryResource(): Result<MaterializedBookResource> =
        Result.success(
            object : MaterializedBookResource {
                override val file = publicationFile
                private var closed = false

                override fun close() {
                    if (!closed) leaseCloseCount.incrementAndGet()
                    closed = true
                }
            },
        )

    override fun close() {
        closed = true
    }
}

/** Minimal redistribution-safe fixtures authored specifically for this spike. */
private object EpubFixture {
    fun write(path: Path, version: Int, rtl: Boolean = false): File {
        ZipOutputStream(path.outputStream()).use { zip ->
            zip.writeStored("mimetype", "application/epub+zip")
            zip.write(
                "META-INF/container.xml",
                """
                    <?xml version="1.0"?>
                    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                    </container>
                """.trimIndent(),
            )
            if (version == 2) writeEpub2(zip) else writeEpub3(zip, rtl)
            zip.write("OEBPS/styles/book.css", "body { font-family: serif; }")
            zip.write(
                "OEBPS/images/cover.svg",
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>",
            )
            zip.write("OEBPS/fonts/test.woff", "authored-fixture-font-placeholder")
            zip.write(
                "OEBPS/chapter1.xhtml",
                xhtml("Chapter One", "<h1 id=\"intro\">Chapter One</h1><p><a href=\"chapter2.xhtml\">Next</a></p>"),
            )
            zip.write("OEBPS/chapter2.xhtml", xhtml("Chapter Two", "<h1>Chapter Two</h1>"))
        }
        return path.toFile()
    }

    private fun writeEpub2(zip: ZipOutputStream) {
        zip.write(
            "OEBPS/content.opf",
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="id" version="2.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:identifier id="id">urn:katari:fixture:epub2</dc:identifier><dc:title>Authored EPUB 2</dc:title><dc:language>en</dc:language>
                  </metadata>
                  <manifest>
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/><item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                    <item id="css" href="styles/book.css" media-type="text/css"/><item id="image" href="images/cover.svg" media-type="image/svg+xml"/><item id="font" href="fonts/test.woff" media-type="font/woff"/>
                  </manifest>
                  <spine toc="ncx"><itemref idref="c1"/><itemref idref="c2"/></spine>
                </package>
            """.trimIndent(),
        )
        zip.write(
            "OEBPS/toc.ncx",
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1"><head/><docTitle><text>Authored EPUB 2</text></docTitle><navMap>
                  <navPoint id="part"><navLabel><text>Part One</text></navLabel><content src="chapter1.xhtml"/><navPoint id="chapter"><navLabel><text>Chapter One</text></navLabel><content src="chapter1.xhtml#intro"/></navPoint></navPoint>
                </navMap></ncx>
            """.trimIndent(),
        )
    }

    private fun writeEpub3(zip: ZipOutputStream, rtl: Boolean) {
        val direction = if (rtl) " page-progression-direction=\"rtl\"" else ""
        val language = if (rtl) "ar" else "en"
        zip.write(
            "OEBPS/content.opf",
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="id" version="3.0" xml:lang="$language">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">urn:katari:fixture:epub3</dc:identifier><dc:title>Authored EPUB 3</dc:title><dc:language>$language</dc:language><meta property="dcterms:modified">2026-07-12T00:00:00Z</meta></metadata>
                  <manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="chapter2.xhtml" media-type="application/xhtml+xml"/><item id="css" href="styles/book.css" media-type="text/css"/><item id="image" href="images/cover.svg" media-type="image/svg+xml"/><item id="font" href="fonts/test.woff" media-type="font/woff"/></manifest>
                  <spine$direction><itemref idref="c1"/><itemref idref="c2"/></spine>
                </package>
            """.trimIndent(),
        )
        zip.write(
            "OEBPS/nav.xhtml",
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Navigation</title></head><body><nav epub:type="toc"><ol><li><a href="chapter1.xhtml">Part One</a><ol><li><a href="chapter1.xhtml#intro">Chapter One</a></li></ol></li></ol></nav></body></html>
            """.trimIndent(),
        )
    }

    private fun xhtml(title: String, body: String) =
        """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>$title</title><link rel="stylesheet" href="styles/book.css"/></head><body>$body</body></html>"""

    private fun ZipOutputStream.write(path: String, value: String) {
        putNextEntry(ZipEntry(path))
        write(value.toByteArray())
        closeEntry()
    }

    private fun ZipOutputStream.writeStored(path: String, value: String) {
        val bytes = value.toByteArray()
        val crc = CRC32().apply { update(bytes) }
        putNextEntry(
            ZipEntry(path).apply {
                method = ZipEntry.STORED
                size = bytes.size.toLong()
                compressedSize = bytes.size.toLong()
                this.crc = crc.value
            },
        )
        write(bytes)
        closeEntry()
    }
}
