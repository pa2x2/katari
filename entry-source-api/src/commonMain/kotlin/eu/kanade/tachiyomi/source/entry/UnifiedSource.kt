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
     * Fetch detailed metadata for an entry.
     *
     * @param entry the entry to update.
     * @return the entry with updated details.
     */
    suspend fun getContentDetails(entry: SEntry): SEntry

    /**
     * Get the list of chapters for an entry.
     *
     * @param entry the entry.
     * @return the chapters for the entry.
     */
    suspend fun getChapterList(entry: SEntry): List<SEntryChapter>

    /**
     * Resolve media for a chapter.
     *
     * For manga this returns [EntryMedia.ImagePages]. For anime this returns
     * [EntryMedia.Playback] containing available streams.
     *
     * @param chapter the chapter to resolve media for.
     * @param selection the preferred playback selection (ignored for manga).
     * @return the media payload for the chapter.
     */
    suspend fun getMedia(
        chapter: SEntryChapter,
        selection: PlaybackSelection = PlaybackSelection(),
    ): EntryMedia
}
