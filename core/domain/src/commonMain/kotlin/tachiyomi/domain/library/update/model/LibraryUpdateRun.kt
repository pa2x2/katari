package tachiyomi.domain.library.update.model

data class LibraryUpdateRun(
    val startedAt: Long,
    /** Null while the update runs, or when the app quit before the update ended. */
    val finishedAt: Long?,
    val trigger: LibraryUpdateTrigger,
    val librarySize: Int,
)

enum class LibraryUpdateTrigger {
    AUTOMATIC,
    MANUAL,

    /** Entries picked by the user before any full update left a report for them to fold into. */
    SELECTION,
}
