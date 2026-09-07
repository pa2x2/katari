package mihon.entry.interactions.book.format.epub.preparation

import mihon.entry.interactions.book.format.epub.archive.epubArchiveBytesFile
import java.nio.charset.Charset

/** Small reflowable publication with independently encoded XML and CSS resources. */
internal fun encodedEpubPublicationFile(
    xmlCharset: Charset = Charsets.UTF_8,
    cssCharset: Charset = Charsets.UTF_8,
    chapterName: String = "chapter.xhtml",
    fragment: String = "position",
) = epubArchiveBytesFile(
    mapOf(
        "mimetype" to "application/epub+zip".encodeToByteArray(),
        "META-INF/container.xml" to encodedEpubXml(
            """<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles><rootfile full-path="OPS/package.opf" media-type="application/oebps-package+xml"/></rootfiles>
            </container>""",
            xmlCharset,
        ),
        "OPS/package.opf" to encodedEpubXml(
            """<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="publication-id">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:identifier id="publication-id">urn:uuid:2ebc39a0-cc2c-4a1d-981d-264e4f215bf8</dc:identifier>
                <dc:title>Encoding fixture</dc:title><dc:language>fr</dc:language>
                <meta property="dcterms:modified">2026-09-07T00:00:00Z</meta>
              </metadata>
              <manifest>
                <item id="chapter" href="$chapterName" media-type="application/xhtml+xml"/>
                <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                <item id="style" href="style.css" media-type="text/css"/>
              </manifest>
              <spine><itemref idref="chapter"/></spine>
            </package>""",
            xmlCharset,
        ),
        "OPS/$chapterName" to encodedEpubXml(
            """<html xmlns="http://www.w3.org/1999/xhtml"><head>
              <title>Chapitre été</title><link rel="stylesheet" href="style.css"/>
            </head><body><p id="$fragment">été 日本語</p></body></html>""",
            xmlCharset,
        ),
        "OPS/nav.xhtml" to encodedEpubXml(
            """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
              <head><title>Contents</title></head>
              <body><nav epub:type="toc"><ol><li>
                <a href="$chapterName#$fragment">Été</a>
              </li></ol></nav></body>
            </html>""",
            xmlCharset,
        ),
        "OPS/style.css" to encodedEpubText("""@charset "UTF-8"; p { color: #123456; }""", cssCharset),
    ),
)

private fun encodedEpubXml(text: String, charset: Charset): ByteArray =
    encodedEpubText("""<?xml version="1.0" encoding="${charset.name()}"?>$text""", charset)

private fun encodedEpubText(text: String, charset: Charset): ByteArray = when (charset) {
    Charsets.UTF_16LE -> byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(charset)
    Charsets.UTF_16BE -> byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + text.toByteArray(charset)
    else -> text.toByteArray(charset)
}
