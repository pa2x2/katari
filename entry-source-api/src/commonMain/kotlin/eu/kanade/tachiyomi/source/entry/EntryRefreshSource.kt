package eu.kanade.tachiyomi.source.entry

/** Result of [EntryRefreshSource.refresh]. */
class EntryRefresh(
    val entry: SEntry,
    val chapters: List<SEntryChapter>,
)

/**
 * Optional capability for sources that refresh an entry's details and chapters in one call.
 *
 * Sources without it are refreshed through [UnifiedSource.getContentDetails] and
 * [UnifiedSource.getChapterList].
 */
interface EntryRefreshSource : UnifiedSource {

    /**
     * Refreshes [entry]'s details when [fetchDetails] is set and its chapters when [fetchChapters]
     * is set.
     *
     * A part that was not requested may be returned as given. The host may apply any returned part
     * regardless of the flags, so only return accurate changes. [existingChapters] are the chapters
     * the host has stored; implementations must not mutate them.
     */
    suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh
}
