package tachiyomi.domain.library.update.model

/** What asking for a library update did. */
sealed interface LibraryUpdateStart {
    data object Started : LibraryUpdateStart

    /** Starts once the update of the profile named [aheadProfileName], the last one queued before it, finishes. */
    data class Queued(val aheadProfileName: String) : LibraryUpdateStart

    /** The profile already has an update running or waiting, so nothing was queued. */
    data object AlreadyQueued : LibraryUpdateStart
}
