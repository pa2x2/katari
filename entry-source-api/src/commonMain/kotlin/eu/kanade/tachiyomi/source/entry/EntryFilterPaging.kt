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

/** Position of a requested page relative to the loaded collection window. */
enum class EntryFilterPageLoadDirection {
    INITIAL,
    BEFORE,
    AFTER,
}

/** Reason for loading a page. */
enum class EntryFilterPageLoadReason {
    INITIAL,
    PAGINATION,
    USER_REFRESH,
}

/**
 * One page request using source-owned opaque continuation and navigation values.
 *
 * [initialAnchor] is present only when starting or refreshing a paging generation at a source-defined navigation
 * destination. Implementations should return a page beginning at or near that destination. It has no meaning outside
 * the same collection view and must not be persisted. [continuationToken] identifies a boundary before or after the
 * requested page according to [direction].
 */
data class EntryFilterPageRequest(
    val scope: EntryFilterPageScope,
    val query: String?,
    val continuationToken: String?,
    val requestedSize: Int,
    val reason: EntryFilterPageLoadReason,
    val initialAnchor: String? = null,
    val direction: EntryFilterPageLoadDirection = if (continuationToken == null) {
        EntryFilterPageLoadDirection.INITIAL
    } else {
        EntryFilterPageLoadDirection.AFTER
    },
) {
    init {
        require(requestedSize in 1..EntryFilterPagingOptions.MAXIMUM_PAGE_SIZE) {
            "requestedSize must be between 1 and ${EntryFilterPagingOptions.MAXIMUM_PAGE_SIZE}"
        }
        when (direction) {
            EntryFilterPageLoadDirection.INITIAL -> {
                require(continuationToken == null) { "INITIAL must not have a continuation token" }
                require(reason != EntryFilterPageLoadReason.PAGINATION) {
                    "INITIAL must start an initial or user-refresh generation"
                }
            }
            EntryFilterPageLoadDirection.BEFORE,
            EntryFilterPageLoadDirection.AFTER,
            -> {
                require(!continuationToken.isNullOrEmpty()) { "$direction requires a continuation token" }
                require(reason == EntryFilterPageLoadReason.PAGINATION) {
                    "$direction must use the PAGINATION load reason"
                }
            }
        }
        require(initialAnchor == null || initialAnchor.isNotEmpty()) { "initialAnchor must not be empty" }
        require(direction == EntryFilterPageLoadDirection.INITIAL || initialAnchor == null) {
            "$direction must not have an initial anchor"
        }
    }
}

/** Request for optional source-defined navigation through one collection view. */
data class EntryFilterNavigationRequest(
    val scope: EntryFilterPageScope,
    val query: String?,
)

/** One source-defined destination within an ordered paged collection. */
data class EntryFilterNavigationTarget(
    val id: String,
    val label: String,
    val anchor: String,
) {
    init {
        require(id.isNotEmpty()) { "id must not be empty" }
        require(label.isNotEmpty()) { "label must not be empty" }
        require(anchor.isNotEmpty()) { "anchor must not be empty" }
    }
}

/** Optional navigation metadata for an ordered paged collection. */
data class EntryFilterNavigation(
    val targets: List<EntryFilterNavigationTarget> = emptyList(),
) {
    init {
        require(targets.distinctBy(EntryFilterNavigationTarget::id).size == targets.size) {
            "navigation target IDs must be unique"
        }
    }
}

/**
 * Immutable source item which a paged group projects into an interactive filter control.
 *
 * [navigationTargetId] optionally associates the item with one of the IDs returned by
 * [EntryFilter.PagedGroup.getNavigation]. The host uses it only to reflect the current navigation destination.
 */
data class EntryFilterPageItem(
    val id: String,
    val label: String,
    val value: String = id,
    val navigationTargetId: String? = null,
) {
    init {
        require(id.isNotEmpty()) { "id must not be empty" }
        require(label.isNotEmpty()) { "label must not be empty" }
        require(navigationTargetId == null || navigationTargetId.isNotEmpty()) {
            "navigationTargetId must not be empty"
        }
    }
}

/**
 * One source-ordered page of filter items with opaque boundaries for adjacent pages.
 *
 * An anchored initial page should provide [previousContinuationToken] when items exist before it so the host can
 * preserve access to the complete collection after a jump.
 */
data class EntryFilterPage(
    val items: List<EntryFilterPageItem>,
    val nextContinuationToken: String? = null,
    val previousContinuationToken: String? = null,
) {
    init {
        require(nextContinuationToken == null || nextContinuationToken.isNotEmpty()) {
            "nextContinuationToken must not be empty"
        }
        require(previousContinuationToken == null || previousContinuationToken.isNotEmpty()) {
            "previousContinuationToken must not be empty"
        }
    }
}
