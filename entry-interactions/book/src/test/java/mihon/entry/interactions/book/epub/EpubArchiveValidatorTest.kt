package mihon.entry.interactions.book.epub

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookFailureReason
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpubArchiveValidatorTest {
    @Test
    fun `accepts a bounded EPUB archive`() = runTest {
        val file = epub(
            "mimetype" to "application/epub+zip",
            "META-INF/container.xml" to "<container/>",
            "EPUB/chapter.xhtml" to "<html><body>Chapter</body></html>",
        )

        assertNull(EpubArchiveValidator().validate(file))
    }

    @Test
    fun `rejects missing required EPUB structure`() = runTest {
        val failure = assertNotNull(
            EpubArchiveValidator().validate(epub("mimetype" to "application/epub+zip")),
        )

        assertEquals(BookFailureReason.MALFORMED_CONTENT, failure.reason)
        assertTrue(failure.message.contains("container.xml"))
    }

    @Test
    fun `rejects unsafe archive paths`() = runTest {
        val unsafe = EpubArchiveValidator().validate(
            epub(
                "mimetype" to "application/epub+zip",
                "META-INF/container.xml" to "<container/>",
                "../outside.xhtml" to "outside",
            ),
        )
        assertTrue(assertNotNull(unsafe).message.contains("parent-directory"))
    }

    @Test
    fun `rejects entry count expanded size and suspicious compression`() = runTest {
        val tooMany = EpubArchiveValidator(EpubArchiveLimits(maxEntries = 2)).validate(
            epub(
                "mimetype" to "application/epub+zip",
                "META-INF/container.xml" to "<container/>",
                "EPUB/chapter.xhtml" to "chapter",
            ),
        )
        assertTrue(assertNotNull(tooMany).message.contains("too many"))

        val expanded = EpubArchiveValidator(EpubArchiveLimits(maxUncompressedBytes = 40)).validate(
            epub(
                "mimetype" to "application/epub+zip",
                "META-INF/container.xml" to "<container>oversized</container>",
            ),
        )
        assertTrue(assertNotNull(expanded).message.contains("expanded-size"))

        val compressed = EpubArchiveValidator(
            EpubArchiveLimits(
                maxCompressionRatio = 2,
                compressionRatioMinimumBytes = 100,
            ),
        ).validate(
            epub(
                "mimetype" to "application/epub+zip",
                "META-INF/container.xml" to "<container/>",
                "EPUB/bomb.xhtml" to "a".repeat(10_000),
            ),
        )
        assertTrue(assertNotNull(compressed).message.contains("suspiciously compressed"))
    }

    @Test
    fun `maps a non-ZIP file to malformed content`() = runTest {
        val file = Files.createTempFile("katari-not-epub", ".epub").toFile().apply {
            writeText("not a ZIP")
        }

        val failure = assertNotNull(EpubArchiveValidator().validate(file))

        assertEquals(BookFailureReason.MALFORMED_CONTENT, failure.reason)
    }

    private fun epub(vararg entries: Pair<String, String>): File {
        return Files.createTempFile("katari-epub-archive", ".epub").toFile().also { file ->
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                entries.forEach { (name, contents) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(contents.encodeToByteArray())
                    zip.closeEntry()
                }
            }
        }
    }
}
