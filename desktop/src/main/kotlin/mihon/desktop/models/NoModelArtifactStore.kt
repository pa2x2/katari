package mihon.desktop.models

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.api.state.StoredModelArtifact

/**
 * A store that holds no model and downloads none: desktop has no engine that needs one yet, so nothing asks for a
 * download. Stands in until desktop has engines with downloadable models.
 */
@Inject
class NoModelArtifactStore : ModelArtifactStore {
    override fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> =
        flowOf(ModelArtifactState.NotInstalled)

    override suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? = null

    override fun download(approval: ModelArtifactDownloadApproval) = Unit

    override fun cancel(artifact: ModelArtifactDescriptor) = Unit

    override suspend fun delete(artifact: ModelArtifactId) = Unit

    override fun observeStored(): Flow<List<StoredModelArtifact>> = flowOf(emptyList())
}
