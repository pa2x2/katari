package tachiyomi.domain.library.update.model

data class LibraryUpdateRun(
    val startedAt: Long,
    /** Null while the update runs, or when it was stopped before finishing. */
    val finishedAt: Long?,
    val trigger: LibraryUpdateTrigger,
    val librarySize: Int,
)

enum class LibraryUpdateTrigger {
    AUTOMATIC,
    MANUAL,

    /** Entries picked by the user, such as a library selection or the entries the previous update skipped. */
    SELECTION,
}
