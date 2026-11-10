package app.katari.webview.chromium

import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.descriptor.ModelArtifactId
import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.model.artifacts.api.state.InstalledModelArtifact
import app.katari.model.artifacts.api.state.ModelArtifactState
import app.katari.model.artifacts.api.state.StoredModelArtifact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File

/** A store whose download ends at once: with the archive in [directory], or as [failure] says. */
class FakeArchiveStore(private val directory: File, private val failure: ModelArtifactState.Failed? = null) :
    ModelArtifactStore {
    private val state = MutableStateFlow<ModelArtifactState>(ModelArtifactState.NotInstalled)

    val archive: File
        get() = File(directory, ChromiumRelease.ARCHIVE_NAME)

    override fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> = state

    override suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? = null

    override fun download(approval: ModelArtifactDownloadApproval) {
        state.value = failure ?: run {
            directory.mkdirs()
            archive.writeText("archive")
            ModelArtifactState.Installed(InstalledModelArtifact(approval.artifact, directory))
        }
    }

    override fun cancel(artifact: ModelArtifactDescriptor) = Unit

    override suspend fun delete(artifact: ModelArtifactId) {
        archive.delete()
        state.value = ModelArtifactState.NotInstalled
    }

    override fun observeStored(): Flow<List<StoredModelArtifact>> = flowOf(emptyList())
}
