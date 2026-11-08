package mihon.feature.library.update.planning

import eu.kanade.tachiyomi.source.entry.EntryType

sealed interface LibraryUpdateRequest {
    /** False for updates of part of the library, whose results fold into the report of the last full update. */
    val coversLibrary: Boolean

    /** Entries the update rules allow, limited to one page of the library when a scope is set. */
    data class FollowRules(
        val automatic: Boolean,
        val categoryId: Long? = null,
        val sourceId: Long? = null,
        val entryType: EntryType? = null,
    ) : LibraryUpdateRequest {
        val isScoped: Boolean
            get() = categoryId != null || sourceId != null || entryType != null

        override val coversLibrary: Boolean
            get() = !isScoped
    }

    /** Entries the user picked. Skip rules and switches don't apply; only an entry's own Never mode keeps it out. */
    data class Selection(val entryIds: Set<Long>) : LibraryUpdateRequest {
        override val coversLibrary: Boolean
            get() = false
    }
}
