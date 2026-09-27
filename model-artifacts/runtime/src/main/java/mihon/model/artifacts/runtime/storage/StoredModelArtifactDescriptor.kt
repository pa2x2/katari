package mihon.model.artifacts.runtime.storage

import kotlinx.serialization.Serializable
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense

/** Persisted form of a [ModelArtifactDescriptor], kept beside the files it describes. */
@Serializable
internal data class StoredModelArtifactDescriptor(
    val id: String,
    val revision: String,
    val displayName: String,
    val files: List<StoredModelArtifactFile>,
    val licenseName: String,
    val licenseUrl: String,
    val sourceUrl: String,
    val upstreamUrl: String? = null,
) {
    fun toDescriptor(): ModelArtifactDescriptor = ModelArtifactDescriptor(
        id = ModelArtifactId(id),
        revision = revision,
        displayName = displayName,
        files = files.map { ModelArtifactFile(it.name, it.url, it.sizeBytes, it.sha256) },
        license = ModelArtifactLicense(licenseName, licenseUrl),
        hosting = upstreamUrl
            ?.let { ModelArtifactHosting.Project(sourceUrl = sourceUrl, upstreamUrl = it) }
            ?: ModelArtifactHosting.Upstream(sourceUrl),
    )
}

@Serializable
internal data class StoredModelArtifactFile(
    val name: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
)

internal fun ModelArtifactDescriptor.toStored(): StoredModelArtifactDescriptor = StoredModelArtifactDescriptor(
    id = id.value,
    revision = revision,
    displayName = displayName,
    files = files.map { StoredModelArtifactFile(it.name, it.url, it.sizeBytes, it.sha256) },
    licenseName = license.name,
    licenseUrl = license.url,
    sourceUrl = hosting.sourceUrl,
    upstreamUrl = (hosting as? ModelArtifactHosting.Project)?.upstreamUrl,
)
