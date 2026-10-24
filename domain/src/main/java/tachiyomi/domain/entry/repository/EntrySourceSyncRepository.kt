package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.Entry

interface EntrySourceSyncRepository {
    /**
     * Writes what a refresh of [entry] changed. Its title is written only with [updateTitle], so a title stored since
     * [entry] was read, such as the name a merged entry's listing gives its member, stays.
     */
    suspend fun updateFromSourceSync(
        entry: Entry,
        profileId: Long,
        updateDateAdded: Boolean,
        updateTitle: Boolean,
    ): Entry?

    /** Whether [entryId] has stored chapters or listed members, after which whether it is a merged entry is fixed. */
    suspend fun hasStoredChildren(entryId: Long): Boolean

    /** Whether a merged entry's source lists [entryId] as a member, whose title then comes from that listing. */
    suspend fun isListedBySourceMerge(entryId: Long, profileId: Long): Boolean
}
