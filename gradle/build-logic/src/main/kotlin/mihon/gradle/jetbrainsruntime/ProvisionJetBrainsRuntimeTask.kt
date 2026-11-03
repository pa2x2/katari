package mihon.gradle.jetbrainsruntime

import mihon.gradle.download.downloadPinned
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import javax.inject.Inject

/**
 * Downloads the JetBrains Runtime from [url], checks it against [sha512] and unpacks it to [home], unless an earlier
 * build on this machine did. The unpacked runtime is moved into place whole, so [home] existing means it is complete;
 * a lock beside it keeps builds of other worktrees from unpacking it at the same time. Its archive keeps symbolic
 * links, which the system's `tar` restores and Gradle's own archive reading does not.
 */
@UntrackedTask(because = "Its output is in the Gradle user home, shared by every build on the machine")
abstract class ProvisionJetBrainsRuntimeTask : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    /** The archive's SHA-512 sum, as JetBrains publishes it beside the archive. */
    @get:Input
    abstract val sha512: Property<String>

    @get:Internal
    abstract val home: DirectoryProperty

    /** Whether Gradle runs with `--offline`, which leaves a runtime that isn't unpacked yet unreachable. */
    @get:Internal
    abstract val offline: Property<Boolean>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun provision() {
        val home = home.get().asFile
        val cache = home.parentFile.also { it.mkdirs() }
        FileChannel.open(
            cache.resolve("${home.name}.lock").toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        ).use { channel ->
            channel.lock().use {
                if (home.isDirectory) return
                if (offline.get()) {
                    throw GradleException(
                        "The JetBrains Runtime isn't downloaded yet and Gradle runs offline. Run the build online " +
                            "once, or run with -PsystemJdk to start on the JDK running Gradle.",
                    )
                }
                val archive = cache.resolve("${home.name}.tar.gz")
                try {
                    download(archive)
                    unpack(archive, home)
                } finally {
                    archive.delete()
                }
            }
        }
    }

    private fun download(archive: File) {
        downloadPinned("the JetBrains Runtime", url.get(), archive, "SHA-512", sha512.get(), logger)
    }

    private fun unpack(archive: File, home: File) {
        val unpacked = Files.createTempDirectory(home.parentFile.toPath(), "${home.name}.").toFile()
        try {
            execOperations.exec { commandLine("tar", "-xzf", archive.absolutePath, "-C", unpacked.absolutePath) }
            val root = unpacked.listFiles()?.singleOrNull { it.isDirectory }
                ?: throw GradleException("The JetBrains Runtime archive from ${url.get()} has no single top folder")
            Files.move(root.toPath(), home.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            unpacked.deleteRecursively()
        }
    }
}
