import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PluginArtifactSanitizerTest {
    @Test
    fun `stale XmlUtil service is removed without changing other entries`() {
        val input = zipOf(
            "nl/adaptivity/xmlutil/XML.class" to "xmlutil",
            STALE_XMLUTIL_SERVICE to "missing.Provider",
        )
        val output = ByteArrayOutputStream()

        removeZipEntry(ByteArrayInputStream(input), output, STALE_XMLUTIL_SERVICE)

        val entries = unzip(output.toByteArray())
        assertFalse(STALE_XMLUTIL_SERVICE in entries)
        assertEquals("xmlutil", entries["nl/adaptivity/xmlutil/XML.class"])
    }

    @Test
    fun `merged Java resource archive is sanitized in place`() {
        val archive = Files.createTempFile("merged-java-resources", ".jar").toFile()
        archive.writeBytes(
            zipOf(
                "unrelated.txt" to "retained",
                STALE_XMLUTIL_SERVICE to "missing.Provider",
            ),
        )

        try {
            sanitizeMergedJavaResources(archive)

            val entries = unzip(archive.readBytes())
            assertFalse(STALE_XMLUTIL_SERVICE in entries)
            assertEquals("retained", entries["unrelated.txt"])
        } finally {
            archive.delete()
        }
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        return ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray())
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    private fun unzip(archive: ByteArray): Map<String, String> {
        return buildMap {
            ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
                generateSequence(zip::getNextEntry).forEach { entry ->
                    put(entry.name, zip.readBytes().decodeToString())
                    zip.closeEntry()
                }
            }
        }
    }
}
