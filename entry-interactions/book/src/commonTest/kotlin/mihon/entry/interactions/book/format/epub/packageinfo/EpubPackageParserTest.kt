package mihon.entry.interactions.book.format.epub.packageinfo

import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.epubArchiveFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EpubPackageParserTest {
    @Test
    fun `expanded XML names distinguish metadata from unrelated elements regardless of prefix`() {
        val file = epubArchiveFile(
            mapOf(
                "META-INF/container.xml" to """
                    <ocf:container xmlns:ocf="urn:oasis:names:tc:opendocument:xmlns:container">
                      <ocf:rootfiles><ocf:rootfile full-path="package.opf"/></ocf:rootfiles>
                    </ocf:container>
                """.trimIndent(),
                "package.opf" to """
                    <opf:package xmlns:opf="http://www.idpf.org/2007/opf" unique-identifier="uid">
                      <opf:metadata xmlns:terms="http://purl.org/dc/elements/1.1/" xmlns:other="urn:other">
                        <other:identifier id="uid">wrong identifier</other:identifier>
                        <other:language>de</other:language>
                        <terms:identifier id="uid">urn:uuid:book</terms:identifier>
                        <language xmlns="http://purl.org/dc/elements/1.1/">pl</language>
                      </opf:metadata>
                      <opf:manifest><opf:item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/></opf:manifest>
                      <opf:spine><opf:itemref idref="chapter"/></opf:spine>
                    </opf:package>
                """.trimIndent(),
                "chapter.xhtml" to "<html><body><p>Text</p></body></html>",
            ),
        )
        try {
            EpubArchive(file).use { archive ->
                val publication = EpubPackageParser(archive).parse()
                assertEquals("urn:uuid:book", publication.uniqueIdentifier)
                assertEquals(listOf("pl"), publication.languages)
                assertEquals(listOf("chapter.xhtml"), publication.readingOrder.map { it.resourceId })
            }
        } finally {
            file.delete()
        }
    }
}
