package eu.kanade.tachiyomi.source.entry

/** Host paging policy for an [EntryFilter.PagedGroup]. */
data class EntryFilterPagingOptions(
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val search: EntryFilterPagingSearchOptions? = EntryFilterPagingSearchOptions(),
) {
    init {
        require(pageSize in 1..MAXIMUM_PAGE_SIZE) {
            "pageSize must be between 1 and $MAXIMUM_PAGE_SIZE"
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val MAXIMUM_PAGE_SIZE = 200
    }
}

/** Source-specific scheduling policy for provider-backed paged-group search. */
data class EntryFilterPagingSearchOptions(
    val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    val minimumQueryLength: Int = DEFAULT_MINIMUM_QUERY_LENGTH,
) {
    init {
        require(debounceMillis >= 0) { "debounceMillis must not be negative" }
        require(minimumQueryLength > 0) { "minimumQueryLength must be positive" }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 300L
        const val DEFAULT_MINIMUM_QUERY_LENGTH = 1
    }
}

/** Collection projection requested from an [EntryFilter.PagedGroup]. */
enum class EntryFilterPageScope {
    AVAILABLE,
    SELECTED,
}

/** Reason for loading a page. */
enum class EntryFilterPageLoadReason {
    INITIAL,
    APPEND,
    USER_REFRESH,
}

/** One page request using a source-owned opaque continuation token. */
data class EntryFilterPageRequest(
    val scope: EntryFilterPageScope,
    val query: String?,
    val continuationToken: String?,
    val requestedSize: Int,
    val reason: EntryFilterPageLoadReason,
) {
    init {
        require(requestedSize in 1..EntryFilterPagingOptions.MAXIMUM_PAGE_SIZE) {
            "requestedSize must be between 1 and ${EntryFilterPagingOptions.MAXIMUM_PAGE_SIZE}"
        }
        when (reason) {
            EntryFilterPageLoadReason.INITIAL,
            EntryFilterPageLoadReason.USER_REFRESH,
            -> require(continuationToken == null) { "$reason must not have a continuation token" }
            EntryFilterPageLoadReason.APPEND -> require(!continuationToken.isNullOrEmpty()) {
                "APPEND requires a continuation token"
            }
        }
    }
}

/** Immutable source item which a paged group projects into an interactive filter control. */
data class EntryFilterPageItem(
    val id: String,
    val label: String,
    val value: String = id,
) {
    init {
        require(id.isNotEmpty()) { "id must not be empty" }
        require(label.isNotEmpty()) { "label must not be empty" }
    }
}

/** One source-ordered page of filter items. */
data class EntryFilterPage(
    val items: List<EntryFilterPageItem>,
    val nextContinuationToken: String? = null,
) {
    init {
        require(nextContinuationToken == null || nextContinuationToken.isNotEmpty()) {
            "nextContinuationToken must not be empty"
        }
    }
}
