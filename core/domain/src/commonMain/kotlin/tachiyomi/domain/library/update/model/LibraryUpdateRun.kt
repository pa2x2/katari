package tachiyomi.domain.library.update.model

data class LibraryUpdateRun(
    val startedAt: Long,
    /** Null while the update runs, or when the app quit before the update ended. */
    val finishedAt: Long?,
    val trigger: LibraryUpdateTrigger,
    val librarySize: Int,
) {
    /** False for an update of entries the user picked, which says nothing of when the rest of the library was checked. */
    val coversLibrary: Boolean
        get() = trigger != LibraryUpdateTrigger.SELECTION

    /** When the update ended, or when it started if the app quit before it ended. */
    val endedAt: Long
        get() = finishedAt ?: startedAt
}

enum class LibraryUpdateTrigger {
    AUTOMATIC,
    MANUAL,

    /** Entries picked by the user before any full update left a report for them to fold into. */
    SELECTION,
}
