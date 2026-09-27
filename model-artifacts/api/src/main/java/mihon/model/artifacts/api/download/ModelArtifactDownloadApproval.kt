package mihon.model.artifacts.api.download

import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor

/**
 * A user's explicit consent to download [artifact] after its size, license, and hosting were disclosed.
 *
 * Hosts must only construct approvals in response to a user decision; nothing downloads by default.
 */
data class ModelArtifactDownloadApproval(
    val artifact: ModelArtifactDescriptor,
    val allowMeteredNetwork: Boolean,
)
