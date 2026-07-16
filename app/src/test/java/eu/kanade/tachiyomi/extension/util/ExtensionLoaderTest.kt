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
    fun `legacy extensions use the platform delegate-last loader when supported`() {
        ExtensionLoader.shouldUseDelegateLastClassLoader("1.4", Build.VERSION_CODES.O_MR1) shouldBe true
        ExtensionLoader.shouldUseDelegateLastClassLoader("1.6.0", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe true
    }

    @Test
    fun `entry extensions retain the custom loader`() {
        ExtensionLoader.shouldUseDelegateLastClassLoader("2.0", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe false
        ExtensionLoader.shouldUseDelegateLastClassLoader("2.0.1", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe false
        ExtensionLoader.shouldUseDelegateLastClassLoader("2.1.0", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe false
        ExtensionLoader.shouldUseDelegateLastClassLoader("2.2.0", Build.VERSION_CODES.VANILLA_ICE_CREAM) shouldBe false
    }

    @Test
    fun `released entry api families are accepted`() {
        ExtensionLoader.isLibVersionCompatible("1.9.1") shouldBe false
        ExtensionLoader.isLibVersionCompatible("2.0.1") shouldBe true
        ExtensionLoader.isLibVersionCompatible("2.1.1") shouldBe true
        ExtensionLoader.isLibVersionCompatible("2.2.1") shouldBe true
        ExtensionLoader.isLibVersionCompatible("2.3.1") shouldBe false

        ExtensionLoader.isRawLibVersionCompatible("2.0.99") shouldBe true
        ExtensionLoader.isRawLibVersionCompatible("2.2.99") shouldBe true
    }

    @Test
    fun `legacy extensions retain the custom loader before Android 8_1`() {
        ExtensionLoader.shouldUseDelegateLastClassLoader("1.4", Build.VERSION_CODES.O) shouldBe false
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
