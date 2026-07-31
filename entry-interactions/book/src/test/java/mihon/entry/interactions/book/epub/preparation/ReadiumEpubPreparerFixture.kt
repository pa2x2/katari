package mihon.entry.interactions.book.epub

import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookByteRange
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.MaterializedBookResource
import mihon.entry.interactions.book.OpenedBookResource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
internal class TestContentSession(
    private val publicationFile: File,
    override val publicationId: String,
    override val revision: String,
    capabilities: Set<BookResourceCapability> = setOf(BookResourceCapability.MATERIALIZE),
) : BookContentSession {
    override val descriptor = BookContentDescriptor(format = "application/epub+zip")
    override val catalogRevision: String? = null
    override val catalogCoverage = BookCatalogCoverage.COMPLETE
    override val resourceHierarchy = emptyList<BookContentResourceGroup>()
    private val resource = BookContentResource(
        id = "publication.epub",
        mediaType = "application/epub+zip",
        size = publicationFile.length(),
        revision = revision,
        cacheState = BookResourceCacheState.CACHED,
        capabilities = capabilities,
    )
    override val primaryResourceIds = listOf(resource.id)
    val leaseCloseCount = AtomicInteger()
    val leaseInvalidationCount = AtomicInteger()
    var closed = false
        private set

    override suspend fun listResources(cursor: String?, limit: Int): Result<BookContentResourcePage> =
        Result.success(BookContentResourcePage(listOf(resource)))

    override suspend fun getResource(resourceId: String): Result<BookContentResource> = if (resourceId == resource.id) {
        Result.success(resource)
    } else {
        Result.failure(NoSuchElementException(resourceId))
    }

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> =
        Result.failure(UnsupportedOperationException("Streaming is not supported by this fixture"))

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> {
        if (resourceId != resource.id) return Result.failure(NoSuchElementException(resourceId))
        return Result.success(
            object : MaterializedBookResource {
                override val metadata = resource
                override val file = publicationFile
                private var closed = false

                override fun invalidate() {
                    leaseInvalidationCount.incrementAndGet()
                }

                override fun close() {
                    if (!closed) leaseCloseCount.incrementAndGet()
                    closed = true
                }
            },
        )
    }

    override fun close() {
        closed = true
    }
}

/** Minimal redistribution-safe fixtures authored specifically for this spike. */
internal object EpubFixture {
    fun write(
        path: Path,
        version: Int,
        rtl: Boolean = false,
        fixedLayout: Boolean = false,
    ): File {
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
            if (version == 2) writeEpub2(zip) else writeEpub3(zip, rtl, fixedLayout)
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

    private fun writeEpub3(zip: ZipOutputStream, rtl: Boolean, fixedLayout: Boolean) {
        val direction = if (rtl) " page-progression-direction=\"rtl\"" else ""
        val language = if (rtl) "ar" else "en"
        val layout = if (fixedLayout) "<meta property=\"rendition:layout\">pre-paginated</meta>" else ""
        zip.write(
            "OEBPS/content.opf",
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="id" version="3.0" xml:lang="$language">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">urn:katari:fixture:epub3</dc:identifier><dc:title>Authored EPUB 3</dc:title><dc:language>$language</dc:language><meta property="dcterms:modified">2026-07-12T00:00:00Z</meta>$layout</metadata>
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
