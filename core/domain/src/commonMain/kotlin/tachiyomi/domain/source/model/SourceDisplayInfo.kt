package tachiyomi.domain.source.model

data class SourceDisplayInfo(
    val id: Long,
    val name: String,
    val lang: String,
    val isMissing: Boolean,
    /** False when the source isn't installed and its name was never recorded, so [name] falls back to its id. */
    val hasKnownName: Boolean,
)
