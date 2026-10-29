package mihon.extension.runtime.jvm.loading

import android.os.Bundle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExtensionJarClassLoaderTest {

    @TempDir
    lateinit var directory: Path

    // Extensions bundle copies of what the app also has: Kotlin's runtime, or an Android class from a library. On ART
    // the boot class path's Android classes always win, a kind's parent-first packages come from the app, and any other
    // bundled copy wins over the app's. Get the order wrong and an Entry SDK source's Kotlin types are the extension's
    // own, failing with a ClassCastException where they cross into the app, or a bundled Android class replaces the
    // stand-in the app hands it. The jar holds the real class files of the copies, taken from the test's classpath.
    @Test
    fun `bundled copies lose to Android's classes and to the kind's parent-first packages, and win over the rest`() {
        val jar = jarOf(Unit::class.java, Bundle::class.java)
        val appLoader = javaClass.classLoader

        val entrySdk = ExtensionJarClassLoader(jar, appLoader, parentFirstPackagePrefixes = listOf("kotlin."))
        entrySdk.loadClass(Unit::class.java.name) shouldBe Unit::class.java
        entrySdk.loadClass(Bundle::class.java.name) shouldBe Bundle::class.java

        val tachiyomi = ExtensionJarClassLoader(jar, appLoader, parentFirstPackagePrefixes = emptyList())
        val bundledUnit = tachiyomi.loadClass(Unit::class.java.name)
        bundledUnit shouldNotBe Unit::class.java
        bundledUnit.classLoader shouldBe tachiyomi
        tachiyomi.loadClass(Bundle::class.java.name) shouldBe Bundle::class.java
    }

    private fun jarOf(vararg classes: Class<*>): Path {
        val jar = directory.resolve("extension.jar")
        ZipOutputStream(Files.newOutputStream(jar)).use { out ->
            classes.forEach { type ->
                val entry = type.name.replace('.', '/') + ".class"
                out.putNextEntry(ZipEntry(entry))
                checkNotNull(type.classLoader.getResourceAsStream(entry)).use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        return jar
    }
}
