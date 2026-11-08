package tachiyomi.domain.source.model

/** A source that isn't installed while library entries come from it, with how many each profile holds by its ID. */
data class MissingLibrarySource(
    val source: SourceDisplayInfo,
    val countsByProfile: Map<Long, Long>,
)
