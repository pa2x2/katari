package mihon.entry.interactions.book.format.epub.preparation

import mihon.entry.interactions.book.format.epub.EpubContract
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun epubPublicationFile(): File {
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
            """<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OPS/package.opf"/></rootfiles></container>""",
        )
        entry(
            "META-INF/encryption.xml",
            """
            <encryption xmlns:enc="http://www.w3.org/2001/04/xmlenc#">
              <enc:EncryptedData>
                <enc:EncryptionMethod Algorithm="http://www.idpf.org/2008/embedding"/>
                <enc:CipherData><enc:CipherReference URI="OPS/font.otf"/></enc:CipherData>
              </enc:EncryptedData>
            </encryption>
            """.trimIndent(),
        )
        entry(
            "OPS/package.opf",
            """
            <package xmlns="http://www.idpf.org/2007/opf" xmlns:dc="http://purl.org/dc/elements/1.1/" version="3.0" unique-identifier="uid">
              <metadata><dc:identifier id="uid">urn:uuid:test-publication</dc:identifier><dc:language>en</dc:language></metadata>
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
