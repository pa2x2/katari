package tachiyomi.domain.library.update.model

import eu.kanade.tachiyomi.source.entry.EntryType

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
