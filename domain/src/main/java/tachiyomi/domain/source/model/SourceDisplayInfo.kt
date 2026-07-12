package tachiyomi.domain.source.model

data class SourceDisplayInfo(
    val id: Long,
    val name: String,
    val lang: String,
    val isMissing: Boolean,
)
