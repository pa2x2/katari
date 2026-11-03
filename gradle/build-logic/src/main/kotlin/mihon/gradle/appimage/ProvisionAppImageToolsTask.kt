package mihon.gradle.appimage

import mihon.gradle.download.downloadPinned
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Downloads each of [tools] to its file, checks it against its pin and makes it executable, unless an earlier build on this machine did. A file is moved into place only once it checks out, so its
 * existing means it is complete; a lock beside it keeps builds of other worktrees from downloading it at the same time.
 */
@UntrackedTask(because = "Its output is in the Gradle user home, shared by every build on the machine")
abstract class ProvisionAppImageToolsTask : DefaultTask() {

    @get:Input
    abstract val tools: ListProperty<PinnedTool>

    /** Whether Gradle runs with `--offline`, which leaves a tool that isn't downloaded yet unreachable. */
    @get:Internal
    abstract val offline: Property<Boolean>

    @TaskAction
    fun provision() {
        tools.get().forEach(::provision)
    }

    private fun provision(tool: PinnedTool) {
        val file = tool.file
        val cache = file.parentFile.also { it.mkdirs() }
        FileChannel.open(
            cache.resolve("${file.name}.lock").toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        ).use { channel ->
            channel.lock().use {
                if (file.isFile) return
                if (offline.get()) {
                    throw GradleException("${tool.name} isn't downloaded yet and Gradle runs offline")
                }
                val download = cache.resolve("${file.name}.download")
                try {
                    downloadPinned(tool.name, tool.url, download, "SHA-256", tool.sha256, logger)
                    download.setExecutable(true)
                    Files.move(download.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } finally {
                    download.delete()
                }
            }
        }
    }
}
