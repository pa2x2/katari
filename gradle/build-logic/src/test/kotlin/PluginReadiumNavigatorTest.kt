import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PluginReadiumNavigatorTest {
    @Test
    fun `embedded PhotoView is removed without changing other AAR entries`() {
        val input = zipOf(
            "classes.jar" to "navigator",
            "libs/PhotoView-2.3.0.jar" to "duplicate",
            "res/values/values.xml" to "resources",
        )
        val output = ByteArrayOutputStream()

        stripEmbeddedPhotoView(ByteArrayInputStream(input), output)

        val entries = unzip(output.toByteArray())
        assertFalse("libs/PhotoView-2.3.0.jar" in entries)
        assertEquals("navigator", entries["classes.jar"])
        assertEquals("resources", entries["res/values/values.xml"])
    }

    @Test
    fun `archive without embedded PhotoView remains complete`() {
        val input = zipOf("classes.jar" to "other library")
        val output = ByteArrayOutputStream()

        stripEmbeddedPhotoView(ByteArrayInputStream(input), output)

        val entries = unzip(output.toByteArray())
        assertTrue("classes.jar" in entries)
        assertEquals("other library", entries["classes.jar"])
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
