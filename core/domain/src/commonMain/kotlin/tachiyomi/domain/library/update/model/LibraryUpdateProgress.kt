package tachiyomi.domain.library.update.model

/** How far a running library update is; [total] stays 0 until the entries to update are known. */
data class LibraryUpdateProgress(
    val scope: LibraryUpdateScope,
    /** Names of the category, source and type the update is limited to; null when it isn't limited to any. */
    val scopeName: String?,
    val completed: Int,
    val total: Int,
    /** Entries in scope that the skip rules left out. */
    val skipped: Int,
)
