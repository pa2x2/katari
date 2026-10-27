package mihon.model.artifacts.api.state

import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import java.io.File

sealed interface ModelArtifactState {
    data object NotInstalled : ModelArtifactState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : ModelArtifactState {
        init {
            require(downloadedBytes in 0..totalBytes)
        }
    }

    data class Installed(
        val artifact: InstalledModelArtifact,
    ) : ModelArtifactState

    data class Failed(
        val failure: ModelArtifactFailure,
    ) : ModelArtifactState
}

sealed interface ModelArtifactFailure {
    /** The active network is metered and the approval did not allow metered downloads. */
    data object MeteredNetwork : ModelArtifactFailure

    data class InsufficientStorage(
        val requiredBytes: Long,
        val availableBytes: Long,
    ) : ModelArtifactFailure

    data class ChecksumMismatch(
        val fileName: String,
    ) : ModelArtifactFailure

    data class Network(
        val message: String?,
    ) : ModelArtifactFailure

    data class Storage(
        val message: String?,
    ) : ModelArtifactFailure
}

/** A verified, completely installed artifact revision. */
data class InstalledModelArtifact(
    val descriptor: ModelArtifactDescriptor,
    private val directory: File,
) {
    fun file(name: String): File {
        require(descriptor.files.any { it.name == name }) {
            "Model artifact ${descriptor.id.value} has no file '$name'"
        }
        return File(directory, name)
    }
}

/** Storage occupied by one artifact revision, complete or partial. */
data class StoredModelArtifact(
    val descriptor: ModelArtifactDescriptor,
    val storedBytes: Long,
    val complete: Boolean,
)
