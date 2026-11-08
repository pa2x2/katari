package app.katari.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Writes the Android API the desktop extension host runs on: the SDK's `android.jar`, whose methods all throw
 * `"Stub!"`, without the classes the stand-ins module implements. Both end up on one class path, and whichever copy of a
 * class comes first is the one that loads, so a class left in the stub could shadow its implementation.
 *
 * Which classes the module implements is read from its source paths, so the stub exists before the module compiles
 * against it: `android/os/Bundle.java` removes `android/os/Bundle.class` and every class nested in it. A source file
 * must therefore be named after the class it defines, as Java requires and Kotlin's style asks.
 *
 * Classes in packages the JDK owns (`java.*`, most of `javax.*`, `org.w3c.dom`, `org.xml.sax`) are left out too: the
 * JDK's own copies always win over the class path, and the stub's would only mislead the compiler.
 */
@CacheableTask
abstract class AndroidStubJarTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidJar: RegularFileProperty

    /** Source roots of the stand-ins module; only the paths of their `.java` and `.kt` files are read. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val sourceRoots: ConfigurableFileCollection

    @get:OutputFile
    abstract val stubJar: RegularFileProperty

    @TaskAction
    fun write() {
        val implemented = implementedClassPaths(sourceRoots.files)
        val output = stubJar.get().asFile
        output.parentFile.mkdirs()
        ZipFile(androidJar.get().asFile).use { sdk ->
            ZipOutputStream(output.outputStream().buffered()).use { stub ->
                sdk.entries().asSequence()
                    .filter { isStubbed(it.name, implemented) }
                    .sortedBy { it.name }
                    .forEach { entry ->
                        // A fixed time keeps the jar byte-identical between builds of the same inputs.
                        stub.putNextEntry(ZipEntry(entry.name).apply { time = FIXED_ENTRY_TIME })
                        sdk.getInputStream(entry).use { it.copyTo(stub) }
                        stub.closeEntry()
                    }
            }
        }
    }

    companion object {
        private const val FIXED_ENTRY_TIME = 315_532_800_000L // 1980-01-01, the earliest time a zip entry holds

        private val jdkPackagePrefixes = listOf("java/", "javax/", "org/w3c/dom/", "org/xml/sax/")

        // OpenGL ES bindings Android ships under javax that no JDK has.
        private val androidOwnedJdkPrefixes = listOf("javax/microedition/")

        /** `android/os/Bundle` for `android/os/Bundle.java` under any of [roots]. */
        internal fun implementedClassPaths(roots: Iterable<File>): Set<String> = roots
            .filter(File::isDirectory)
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
                    .map { it.relativeTo(root).invariantSeparatorsPath.substringBeforeLast('.') }
                    .toList()
            }
            .toSet()

        /** Whether the stub keeps [entryName] of `android.jar`, given the module's [implemented] class paths. */
        internal fun isStubbed(entryName: String, implemented: Set<String>): Boolean {
            if (!entryName.endsWith(".class")) return false
            if (jdkPackagePrefixes.any(entryName::startsWith) && androidOwnedJdkPrefixes.none(entryName::startsWith)) {
                return false
            }
            val topLevelClass = entryName.removeSuffix(".class").substringBefore('$')
            return topLevelClass !in implemented
        }
    }
}
