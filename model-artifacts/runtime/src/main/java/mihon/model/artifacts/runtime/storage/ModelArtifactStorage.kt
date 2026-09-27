package mihon.model.artifacts.runtime.storage

import kotlinx.serialization.json.Json
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.model.artifacts.api.state.StoredModelArtifact
import java.io.File

/**
 * On-disk layout of model artifacts: `<root>/<id>/<revision>/`.
 *
 * A revision directory holds its descriptor from the moment a download starts, the artifact files, and an
 * installation marker that is written only after every file was verified. The root is resolved on first access so
 * installing the Feature performs no file-system work.
 */
internal class ModelArtifactStorage(
    root: () -> File,
) {
    private val root by lazy(root)
    private val json = Json { ignoreUnknownKeys = true }

    fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? {
        val directory = revisionDirectory(descriptor)
        if (!File(directory, INSTALLED_MARKER).isFile) return null
        if (readDescriptor(directory) != descriptor) return null
        val complete = descriptor.files.all { file ->
            File(directory, file.name).let { it.isFile && it.length() == file.sizeBytes }
        }
        return InstalledModelArtifact(descriptor, directory).takeIf { complete }
    }

    /** Prepares the revision directory for a download, discarding stored content of a different descriptor. */
    fun begin(descriptor: ModelArtifactDescriptor) {
        val directory = revisionDirectory(descriptor)
        if (directory.exists() && readDescriptor(directory) != descriptor) {
            directory.deleteRecursively()
        }
        directory.mkdirs()
        check(directory.isDirectory) { "Cannot create model artifact directory ${directory.path}" }
        File(directory, DESCRIPTOR_FILE).writeText(
            json.encodeToString(StoredModelArtifactDescriptor.serializer(), descriptor.toStored()),
        )
    }

    fun target(descriptor: ModelArtifactDescriptor, file: ModelArtifactFile): File =
        File(revisionDirectory(descriptor), file.name)

    fun storedBytes(descriptor: ModelArtifactDescriptor): Long =
        revisionDirectory(descriptor).takeIf(File::isDirectory)?.let(::directorySize) ?: 0L

    fun usableBytes(): Long {
        root.mkdirs()
        return root.usableSpace
    }

    /** Marks the revision installed and removes every other stored revision of the same artifact. */
    fun commit(descriptor: ModelArtifactDescriptor) {
        val directory = revisionDirectory(descriptor)
        File(directory, INSTALLED_MARKER).writeText(descriptor.revision)
        artifactDirectory(descriptor.id).listFiles()
            ?.filter { it.isDirectory && it != directory }
            ?.forEach(File::deleteRecursively)
    }

    fun delete(artifact: ModelArtifactId) {
        artifactDirectory(artifact).deleteRecursively()
    }

    fun stored(): List<StoredModelArtifact> {
        return root.listFiles().orEmpty()
            .filter(File::isDirectory)
            .flatMap { artifactDirectory -> artifactDirectory.listFiles().orEmpty().filter(File::isDirectory) }
            .mapNotNull { directory ->
                val descriptor = readDescriptor(directory) ?: return@mapNotNull null
                StoredModelArtifact(
                    descriptor = descriptor,
                    storedBytes = directorySize(directory),
                    complete = installed(descriptor) != null,
                )
            }
            .sortedWith(compareBy({ it.descriptor.displayName }, { it.descriptor.revision }))
    }

    private fun readDescriptor(directory: File): ModelArtifactDescriptor? {
        val file = File(directory, DESCRIPTOR_FILE)
        if (!file.isFile) return null
        return runCatching {
            json.decodeFromString(StoredModelArtifactDescriptor.serializer(), file.readText()).toDescriptor()
        }.getOrNull()
    }

    private fun artifactDirectory(artifact: ModelArtifactId): File = File(root, artifact.value)

    private fun revisionDirectory(descriptor: ModelArtifactDescriptor): File =
        File(artifactDirectory(descriptor.id), descriptor.revision)

    private fun directorySize(directory: File): Long =
        directory.walkBottomUp().filter(File::isFile).sumOf(File::length)

    private companion object {
        const val DESCRIPTOR_FILE = "descriptor.json"
        const val INSTALLED_MARKER = "installed"
    }
}
