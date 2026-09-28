package eu.kanade.tachiyomi.extension.util

import android.os.Build
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ExtensionLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `extension lib metadata accepts float double and string values`() {
        ExtensionLoader.getExtensionLibVersion(1.6f) shouldBe "1.6"
        ExtensionLoader.getExtensionLibVersion(1.4) shouldBe "1.4"
        ExtensionLoader.getExtensionLibVersion("2.0.1") shouldBe "2.0.1"
    }

    @Test
    fun `only legacy extensions use the platform delegate-last loader`() {
        ExtensionLoader.shouldUseDelegateLastClassLoader("1.4", Build.VERSION_CODES.Q) shouldBe true
        ExtensionLoader.shouldUseDelegateLastClassLoader("1.6.0", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe true
        ExtensionLoader.shouldUseDelegateLastClassLoader("2.0.1", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe false
    }

    @Test
    fun `extension version names are accepted only within released entry api families`() {
        ExtensionLoader.isLibVersionCompatible("1.9.1") shouldBe false
        ExtensionLoader.isLibVersionCompatible("2.0.1") shouldBe true
        ExtensionLoader.isLibVersionCompatible("2.7.1") shouldBe true
        ExtensionLoader.isLibVersionCompatible("2.8.1") shouldBe false
    }

    @Test
    fun `extension apk cache is read-only and replaces stale files`() {
        val source = tempDir.resolve("source.apk").createFile().apply {
            writeText("extension contents")
        }
        val cacheDir = tempDir.resolve("cache").toFile().apply { mkdirs() }
        val stale = cacheDir.resolve("example.extension-stale.apk").apply { writeText("stale") }

        val cached = ExtensionLoader.cacheExtensionApk(source.toFile(), cacheDir, "example.extension")

        cached.toPath().readText() shouldBe "extension contents"
        cached.canWrite().shouldBeFalse()
        stale.exists().shouldBeFalse()
    }
}
