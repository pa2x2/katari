package app.katari.gradle.appimage

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Links the runtime the AppImage ships: every module of the runtime the app runs on during development, read from
 * [runtimeRelease], except [excludedModules], linked with `jlink` from the SDK of the same JetBrains Runtime build.
 *
 * Extensions are loaded while the app runs and may call any of the platform's APIs, and parts of the platform such as
 * the extra charsets or the locale data are found only at run time, so the runtime keeps everything but the JDK's
 * development and diagnostic tools rather than the modules the app's code is seen to use. The module image is left
 * uncompressed: the AppImage compresses it better than `jlink` does, and the JVM then reads it without unpacking it.
 */
abstract class LinkAppImageRuntimeTask : DefaultTask() {

    /** The SDK's archive name, which names its version and build. */
    @get:Input
    abstract val sdkName: Property<String>

    @get:Internal
    abstract val sdkHome: DirectoryProperty

    /** The `release` file of the runtime the app runs on during development, whose `MODULES` it is linked with. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeRelease: RegularFileProperty

    @get:Input
    abstract val excludedModules: ListProperty<String>

    @get:OutputDirectory
    abstract val runtime: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun link() {
        val modules = runtimeModules(runtimeRelease.get().asFile) - excludedModules.get().toSet()
        val runtime = runtime.get().asFile
        // jlink refuses to write into a folder that exists
        runtime.deleteRecursively()
        val sdk = sdkHome.get().asFile
        execOperations.exec {
            commandLine(
                sdk.resolve("bin/jlink").absolutePath,
                "--module-path", sdk.resolve("jmods").absolutePath,
                "--add-modules", modules.joinToString(","),
                "--compress=zip-0",
                "--no-header-files",
                "--no-man-pages",
                // The JDK's own default class data archive, which starts the JVM faster. Dumping it logs that JFR's
                // event class is skipped, which the JDK does with every archive it makes.
                "--generate-cds-archive",
                "--output", runtime.absolutePath,
            )
        }
        // The archive for heaps over 32 GB, where the JVM turns compressed pointers off and starts without one
        runtime.resolve("lib/server/classes_nocoops.jsa").delete()
        restoreExecutables(from = sdk, to = runtime)
        writeRelease(runtime.resolve("release"), runtimeRelease.get().asFile)
    }

    /**
     * jlink copies the programs a module keeps among its native files, such as JCEF's `cef_server` and `jcef_helper`,
     * without their executable bit, and Chromium can't start without them. Each file gets the bit its copy in the SDK has.
     */
    private fun restoreExecutables(from: File, to: File) {
        to.walkTopDown().filter { it.isFile && !it.canExecute() }.forEach { file ->
            if (from.resolve(file.relativeTo(to)).canExecute()) file.setExecutable(true, false)
        }
    }

    /**
     * The `release` file of the runtime the app runs on during development, with the modules this runtime has: jlink
     * writes only the Java version and the modules, and leaves out the JetBrains Runtime's build and its JCEF version.
     */
    private fun writeRelease(release: File, developmentRelease: File) {
        val modules = release.readLines().single { it.startsWith(MODULES_KEY) }
        release.writeText(
            developmentRelease.readLines().joinToString("\n", postfix = "\n") {
                if (it.startsWith(MODULES_KEY)) modules else it
            },
        )
    }

    companion object {
        private const val MODULES_KEY = "MODULES="

        /**
         * The JDK's tools for developing and diagnosing Java programs: compilers, debuggers, attaching to and inspecting
         * a running JVM, and flight recording. Nothing the app or an extension runs on.
         */
        val DEVELOPMENT_MODULES = listOf(
            "jdk.attach",
            "jdk.compiler",
            "jdk.hotspot.agent",
            "jdk.internal.ed",
            "jdk.internal.jvmstat",
            "jdk.internal.le",
            "jdk.internal.md",
            "jdk.internal.opt",
            "jdk.internal.vm.ci",
            "jdk.javadoc",
            "jdk.jcmd",
            "jdk.jdi",
            "jdk.jdwp.agent",
            "jdk.jfr",
            "jdk.management.agent",
            "jdk.management.jfr",
        )
    }
}
