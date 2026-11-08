package app.katari.gradle.appimage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Fails when the app's jars need a module the AppImage's runtime doesn't have, as a library that starts using one of
 * the JDK's development tools would. `jdeps` reads the jars as the app runs them, on the class path: their module
 * descriptors are left out, as the JVM ignores them there and `jdeps` would otherwise resolve them as modules, which the
 * app's jars are not consistent enough to be. [report] lists the modules they need.
 */
abstract class CheckAppImageModulesTask : DefaultTask() {

    @get:Classpath
    abstract val jars: ConfigurableFileCollection

    /** The SDK's archive name, which names its version and build. */
    @get:Input
    abstract val sdkName: Property<String>

    @get:Internal
    abstract val sdkHome: DirectoryProperty

    /** The `release` file of the runtime the AppImage ships. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeRelease: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun check() {
        val classPath = temporaryDir.resolve("class-path").apply {
            deleteRecursively()
            mkdirs()
        }
        val copies = jars.files.mapIndexed { index, jar ->
            classPath.resolve("$index-${jar.name}").also { copyWithoutModuleDescriptors(jar, it) }
        }
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(
                listOf(
                    sdkHome.get().asFile.resolve("bin/jdeps").absolutePath,
                    "--print-module-deps",
                    "--ignore-missing-deps",
                    // The versioned classes the runtime loads from multi-release jars
                    "--multi-release",
                    runtimeFeatureVersion(runtimeRelease.get().asFile),
                    "--class-path",
                    copies.joinToString(File.pathSeparator),
                ) + copies.map { it.absolutePath },
            )
            standardOutput = output
            errorOutput = output
        }
        classPath.deleteRecursively()
        val lines = output.toString().lines().filter { it.isNotBlank() }
        // jdeps notes the packages that jars repeat from the platform's modules, such as xml-apis' org.w3c.dom, which
        // the JVM loads from the modules whatever the class path holds
        val (notes, results) = lines.partition { it.startsWith("Warning:") }
        notes.forEach { logger.info(it) }
        val needed = results.singleOrNull()?.split(',')?.map { it.trim() }?.toSet()
            ?: throw GradleException("jdeps printed no module list:\n${lines.joinToString("\n")}")
        val missing = needed - runtimeModules(runtimeRelease.get().asFile)
        if (missing.isNotEmpty()) {
            throw GradleException(
                "The desktop app's jars need ${missing.sorted()}, which the AppImage's runtime leaves out. " +
                    "Keep them in it by taking them off LinkAppImageRuntimeTask.DEVELOPMENT_MODULES.",
            )
        }
        report.get().asFile.writeText(needed.sorted().joinToString("\n", postfix = "\n"))
    }

    private fun copyWithoutModuleDescriptors(jar: File, target: File) {
        ZipFile(jar).use { zip ->
            ZipOutputStream(target.outputStream().buffered()).use { out ->
                zip.entries().asSequence().filterNot { it.name.endsWith("module-info.class") }.forEach { entry ->
                    out.putNextEntry(ZipEntry(entry.name))
                    zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
    }
}
