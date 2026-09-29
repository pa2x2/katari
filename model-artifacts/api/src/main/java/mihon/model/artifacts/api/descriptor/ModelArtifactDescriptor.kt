package mihon.model.artifacts.api.descriptor

@JvmInline
value class ModelArtifactId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid model artifact id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

/**
 * One immutable revision of a downloadable model.
 *
 * [revision] identifies the exact published content (for example an upstream commit). Changing any file requires a new
 * revision so installed content can never silently diverge from its checksums.
 */
data class ModelArtifactDescriptor(
    val id: ModelArtifactId,
    val revision: String,
    val displayName: String,
    val files: List<ModelArtifactFile>,
    val license: ModelArtifactLicense,
    val hosting: ModelArtifactHosting,
) {
    init {
        require(REVISION_PATTERN.matches(revision)) { "Invalid model artifact revision: '$revision'" }
        require(displayName.isNotBlank())
        require(files.isNotEmpty())
        require(files.map(ModelArtifactFile::name).toSet().size == files.size) {
            "Model artifact ${id.value} declares duplicate file names"
        }
    }

    val sizeBytes: Long
        get() = files.sumOf(ModelArtifactFile::sizeBytes)

    private companion object {
        val REVISION_PATTERN = Regex("""[A-Za-z0-9][A-Za-z0-9._-]*""")
    }
}

/**
 * One file of an artifact. [name] is the artifact-relative storage name; [sha256] is the lowercase hex digest the
 * downloaded content must match before the artifact is considered installed.
 */
data class ModelArtifactFile(
    val name: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
) {
    init {
        require(NAME_PATTERN.matches(name) && name.split('/').none { it == "." || it == ".." }) {
            "Invalid model artifact file name: '$name'"
        }
        require(url.startsWith("https://")) { "Model artifact files must be downloaded over HTTPS: '$url'" }
        require(sizeBytes > 0)
        require(SHA256_PATTERN.matches(sha256)) { "Invalid SHA-256 digest for '$name'" }
    }

    private companion object {
        val NAME_PATTERN = Regex("""[A-Za-z0-9._-]+(?:/[A-Za-z0-9._-]+)*""")
        val SHA256_PATTERN = Regex("""[0-9a-f]{64}""")
    }
}

data class ModelArtifactLicense(
    val name: String,
    val url: String,
) {
    init {
        require(name.isNotBlank())
        require(url.isNotBlank())
    }
}

/** Where the artifact is published, disclosed to the user before approval. */
sealed interface ModelArtifactHosting {
    val sourceUrl: String

    /** Published by the model's authors or an upstream conversion maintained outside this project. */
    data class Upstream(
        override val sourceUrl: String,
    ) : ModelArtifactHosting {
        init {
            require(sourceUrl.isNotBlank())
        }
    }

    /** Re-published by this project because no usable upstream publication exists. */
    data class Project(
        override val sourceUrl: String,
        val upstreamUrl: String,
    ) : ModelArtifactHosting {
        init {
            require(sourceUrl.isNotBlank())
            require(upstreamUrl.isNotBlank())
        }
    }
}
