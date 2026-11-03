package tachiyomi.core.common.preference

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class DesktopPreferenceStoreTest {

    @TempDir
    lateinit var directory: Path

    @Test
    fun `values read back after a restart with the type they were written with`() {
        val file = directory.resolve("preferences.json")
        DesktopPreferenceStore(file).apply {
            getString("string").set("text")
            getLong("long").set(5L)
            getInt("int").set(7)
            getFloat("float").set(0.1f)
            getBoolean("boolean").set(true)
            getStringSet("set").set(setOf("a", "b"))
            flush()
        }

        // A typed getter finding another type deletes the value and returns its default, so these fail on a type lost.
        DesktopPreferenceStore(file).apply {
            getString("string").get() shouldBe "text"
            getLong("long").get() shouldBe 5L
            getInt("int").get() shouldBe 7
            getFloat("float").get() shouldBe 0.1f
            getBoolean("boolean").get() shouldBe true
            getStringSet("set").get() shouldBe setOf("a", "b")
        }
    }

    @Test
    fun `a malformed file starts an empty store that writes a readable file again`() {
        val file = directory.resolve("preferences.json")
        file.writeText("{\"string\": {\"type\": \"string\", \"value\": ")

        DesktopPreferenceStore(file).apply {
            getAll().shouldBeEmpty()
            getString("string").set("text")
            flush()
        }

        DesktopPreferenceStore(file).getString("string").get() shouldBe "text"
    }
}
