package eu.kanade.tachiyomi.source.entry

/**
 * Type-agnostic source contract consumed by the app.
 *
 * Sources may return items of any [EntryType] in a single list. Only
 * type-specific renderers (reader, player, etc.) branch on the actual type.
 */
interface UnifiedSource {

    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): EntryFilterList = EntryFilterList()

    /**
     * Get a page of popular content.
     *
     * @param page the page number to retrieve.
     */
    suspend fun getPopularContent(page: Int): EntryPageResult<SEntry>

    /**
     * Get a page of latest updated content.
     *
     * @param page the page number to retrieve.
     */
    suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry>

    /**
     * Get a page of content matching the query and filters.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry>

    /**
     * Refreshes [entry]'s details when [fetchDetails] is set and its chapters when [fetchChapters]
     * is set.
     *
     * A part that was not requested may be returned as given. The host may apply any returned part
     * regardless of the flags, so only return accurate changes. [existingChapters] are the chapters
     * the host has stored; implementations must not mutate them or assume that refreshes are
     * serialized.
     *
     * Sources implement this. The default only keeps sources built against SDK 2.6 or earlier
     * working by calling their [getContentDetails] and [getChapterList], and fails for a source that
     * implements neither.
     */
    suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh = refreshThroughSeparateCalls(entry, existingChapters, fetchDetails, fetchChapters)

    /** Fetches [entry]'s details. Sources built against SDK 2.6 or earlier implement this instead of [refresh]. */
    @Deprecated(
        "Implement refresh, which fetches details and chapters in one call",
        ReplaceWith("refresh(entry, emptyList(), fetchDetails = true, fetchChapters = false).entry"),
    )
    suspend fun getContentDetails(entry: SEntry): SEntry =
        refreshForSeparateCall(entry, fetchDetails = true).entry

    /** Fetches [entry]'s chapters. Sources built against SDK 2.6 or earlier implement this instead of [refresh]. */
    @Deprecated(
        "Implement refresh, which fetches details and chapters in one call",
        ReplaceWith("refresh(entry, emptyList(), fetchDetails = false, fetchChapters = true).chapters"),
    )
    suspend fun getChapterList(entry: SEntry): List<SEntryChapter> =
        refreshForSeparateCall(entry, fetchDetails = false).chapters

    /**
     * Resolve media for a chapter.
     *
     * For manga this returns [EntryMedia.ImagePages]. For anime this returns
     * [EntryMedia.Playback] containing available streams. For books this returns
     * [EntryMedia.Book] describing readable resources and their access locations.
     *
     * @param chapter the chapter to resolve media for.
     * @param selection the preferred playback selection (ignored for manga and books).
     * @return the media payload for the chapter.
     */
    suspend fun getMedia(
        chapter: SEntryChapter,
        selection: PlaybackSelection = PlaybackSelection(),
    ): EntryMedia
}
