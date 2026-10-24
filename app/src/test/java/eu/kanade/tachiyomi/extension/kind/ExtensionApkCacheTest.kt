package eu.kanade.tachiyomi.extension.kind

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ExtensionApkCacheTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `extension apk cache is read-only and replaces stale files`() {
        val source = tempDir.resolve("source.apk").createFile().apply {
            writeText("extension contents")
        }
        val cacheDir = tempDir.resolve("cache").toFile().apply { mkdirs() }
        val stale = cacheDir.resolve("example.extension-stale.apk").apply { writeText("stale") }

        val cached = cacheExtensionApk(source.toFile(), cacheDir, "example.extension")

        cached.toPath().readText() shouldBe "extension contents"
        cached.canWrite().shouldBeFalse()
        stale.exists().shouldBeFalse()
    }
}
