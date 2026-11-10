package app.katari.gradle.chromium

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Packs the Chromium of the JetBrains Runtime's SDK into the archive the desktop app downloads it from: the native
 * files of the SDK's `jcef` module as [ChromiumFiles] tells them apart, with the [licenses] they are passed on under in
 * `LICENSES/`. The same SDK gives the same archive, byte for byte, so the one published can be told from its checksum.
 */
abstract class AssembleChromiumArchiveTask : DefaultTask() {

    /** The SDK's archive name, which names its version and build. */
    @get:Input
    abstract val sdkName: Property<String>

    @get:Internal
    abstract val sdkHome: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val licenses: DirectoryProperty

    @get:OutputFile
    abstract val archive: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun assemble() {
        val sdk = sdkHome.get().asFile
        val work = temporaryDir.also { it.deleteRecursively() }
        execOperations.exec {
            commandLine(
                sdk.resolve("bin/jmod").absolutePath,
                "extract",
                "--dir",
                work.absolutePath,
                ChromiumFiles.jmod(sdk).absolutePath,
            )
        }
        val chromium = work.resolve(ChromiumFiles.NATIVE_SECTION)
        (ChromiumFiles.KEPT_IN_RUNTIME + ChromiumFiles.LEFT_OUT).forEach { name ->
            if (!chromium.resolve(name).deleteRecursively()) {
                throw GradleException("The SDK's ${ChromiumFiles.MODULE} module has no $name to leave out")
            }
        }
        // jmod keeps no executable bits; each file gets the one its copy in the SDK's own runtime has
        chromium.walkTopDown().filter { it.isFile }.forEach { file ->
            file.setExecutable(sdk.resolve("lib").resolve(file.relativeTo(chromium)).canExecute(), false)
        }
        licenses.get().asFile.copyRecursively(chromium.resolve("LICENSES"))

        val archive = archive.get().asFile
        archive.delete()
        execOperations.exec {
            commandLine(
                "tar",
                // The same bytes whenever and by whomever it is packed
                "--sort=name", "--mtime=@0", "--owner=0", "--group=0", "--numeric-owner",
                "--use-compress-program=zstd -19 -T1",
                "-cf", archive.absolutePath,
                "-C", chromium.absolutePath,
                ".",
            )
        }
        logger.lifecycle("${archive.name}: ${archive.length()} bytes, SHA-256 ${sha256(archive)}")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.forEachBlock { buffer, read -> digest.update(buffer, 0, read) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
