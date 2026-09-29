package mihon.model.artifacts.api

import kotlinx.coroutines.flow.Flow
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.api.state.StoredModelArtifact

/**
 * Application-wide owner of downloadable model artifacts.
 *
 * Consumers describe the artifacts they need; the store never downloads anything without a
 * [ModelArtifactDownloadApproval] that a host created from an explicit user decision.
 */
interface ModelArtifactStore {
    /** Observes the installation state of exactly this artifact revision. */
    fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState>

    /** Returns the verified installation of exactly this artifact revision, or `null` when it is not installed. */
    suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact?

    /**
     * Starts or resumes an approved download. Progress and completion are published through [observe].
     * The download continues when the requesting surface goes away and can be stopped with [cancel].
     */
    fun download(approval: ModelArtifactDownloadApproval)

    fun cancel(artifact: ModelArtifactDescriptor)

    /** Removes every stored revision of [artifact], including partial downloads. */
    suspend fun delete(artifact: ModelArtifactId)

    /** Every artifact revision that currently occupies storage, for storage management surfaces. */
    fun observeStored(): Flow<List<StoredModelArtifact>>
}
