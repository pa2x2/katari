package tachiyomi.domain.updates.model

/**
 * The updates of the feed's window, before the user's feed filters.
 *
 * @param fromHiddenSources updates left out because their source is hidden, counted so the feed can say so.
 */
data class UpdatesFeed(
    val rows: List<UpdatesFeedRow>,
    val fromHiddenSources: Int,
)

/** @param categoryIds the categories of the entry and of the entries merged with it; Default when there are none. */
data class UpdatesFeedRow(
    val update: UpdatesWithRelations,
    val categoryIds: Set<Long>,
)
