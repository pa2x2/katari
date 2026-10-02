package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.entry.EntryType

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

/** Which part of the library an update was asked to cover. */
enum class LibraryUpdateScope {
    Library,
    Category,
    Source,
    Type,

    /** More than one of category, source and entry type. */
    Group,

    /** Entries the user picked. */
    Selection,
    ;

    companion object {
        fun of(categoryId: Long?, sourceId: Long?, entryType: EntryType?): LibraryUpdateScope {
            val constraints = listOfNotNull(
                categoryId?.let { Category },
                sourceId?.let { Source },
                entryType?.let { Type },
            )
            return when (constraints.size) {
                0 -> Library
                1 -> constraints.single()
                else -> Group
            }
        }
    }
}
