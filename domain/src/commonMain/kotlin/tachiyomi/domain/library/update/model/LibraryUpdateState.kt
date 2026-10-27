package tachiyomi.domain.library.update.model

/** One profile's library update, running or waiting its turn. */
sealed interface LibraryUpdateState {
    data class Running(val progress: LibraryUpdateProgress) : LibraryUpdateState

    /** Waits for the update of the profile named [runningProfileName], which runs now. */
    data class Waiting(val runningProfileName: String) : LibraryUpdateState
}
