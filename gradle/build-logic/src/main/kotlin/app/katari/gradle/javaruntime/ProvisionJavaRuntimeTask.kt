package app.katari.gradle.javaruntime

import app.katari.gradle.download.downloadPinned
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.UntrackedTask
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import javax.inject.Inject

/**
 * Downloads a pinned Java runtime from [url], checks it against [digest] and unpacks it to [home], unless an earlier
 * build on this machine did. The unpacked runtime is moved into place whole, so [home] existing means it is complete;
 * a lock beside it keeps builds of other worktrees from unpacking it at the same time. Runtime archives keep symbolic
 * links, which the system's `tar` restores and Gradle's own archive reading does not.
 */
@UntrackedTask(because = "Its output is in the Gradle user home, shared by every build on the machine")
abstract class ProvisionJavaRuntimeTask : DefaultTask() {

    /** Names the runtime in the task's messages, in the middle of a sentence. */
    @get:Internal
    abstract val runtimeName: Property<String>

    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val digestAlgorithm: Property<String>

    @get:Input
    abstract val digest: Property<String>

    @get:Internal
    abstract val home: DirectoryProperty

    /** Whether Gradle runs with `--offline`, which leaves a runtime that isn't unpacked yet unreachable. */
    @get:Internal
    abstract val offline: Property<Boolean>

    /** What a build can do instead when it runs offline before the runtime is downloaded, besides going online. */
    @get:Internal
    abstract val offlineAlternative: Property<String>

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
                    val alternative = offlineAlternative.orNull?.let { ", or $it" }.orEmpty()
                    throw GradleException(
                        "${runtimeName.get().replaceFirstChar(Char::uppercaseChar)} isn't downloaded yet and Gradle " +
                            "runs offline. Run the build online once$alternative.",
                    )
                }
                val archive = cache.resolve("${home.name}.tar.gz")
                try {
                    downloadPinned(runtimeName.get(), url.get(), archive, digestAlgorithm.get(), digest.get(), logger)
                    unpack(archive, home)
                } finally {
                    archive.delete()
                }
            }
        }
    }

    private fun unpack(archive: File, home: File) {
        val unpacked = Files.createTempDirectory(home.parentFile.toPath(), "${home.name}.").toFile()
        try {
            execOperations.exec { commandLine("tar", "-xzf", archive.absolutePath, "-C", unpacked.absolutePath) }
            val root = unpacked.listFiles()?.singleOrNull { it.isDirectory }
                ?: throw GradleException(
                    "The archive of ${runtimeName.get()} from ${url.get()} has no single top folder",
                )
            Files.move(root.toPath(), home.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            unpacked.deleteRecursively()
        }
    }
}

/** This project's task that makes [runtime] ready, registered by the first caller. */
internal fun Project.provisionJavaRuntime(runtime: PinnedJavaRuntime): TaskProvider<ProvisionJavaRuntimeTask> {
    val name = runtime.provisionTaskName
    if (name in tasks.names) return tasks.named(name, ProvisionJavaRuntimeTask::class.java)
    val offline = gradle.startParameter.isOffline
    return tasks.register<ProvisionJavaRuntimeTask>(name) {
        description = "Downloads, verifies and unpacks ${runtime.name}."
        runtimeName.set(runtime.name)
        url.set(runtime.url)
        digestAlgorithm.set(runtime.digestAlgorithm)
        digest.set(runtime.digest)
        home.set(runtime.home)
        this.offline.set(offline)
        offlineAlternative.set(runtime.offlineAlternative)
    }
}
