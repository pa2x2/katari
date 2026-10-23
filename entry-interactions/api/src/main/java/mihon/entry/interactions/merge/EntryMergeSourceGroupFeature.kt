package mihon.entry.interactions.merge

import tachiyomi.domain.entry.model.Entry

/**
 * Keeps the merge of an entry merged by its source in step with the source.
 *
 * The root and every member the source lists are locked; members a user adds stay as they are.
 */
interface EntryMergeSourceGroupFeature {
    /**
     * Applies what [root]'s source listed as its members, in source order. A listed title in another source's merge
     * moves here; one in a merge a user made stays there. Members the source no longer lists leave the merge.
     */
    suspend fun sync(root: Entry, listed: List<Entry>): EntryMergeSourceGroupResult
}

sealed interface EntryMergeSourceGroupResult {
    data object Applied : EntryMergeSourceGroupResult
    data object Conflict : EntryMergeSourceGroupResult
    data class OperationalFailure(val retryable: Boolean) : EntryMergeSourceGroupResult
}
