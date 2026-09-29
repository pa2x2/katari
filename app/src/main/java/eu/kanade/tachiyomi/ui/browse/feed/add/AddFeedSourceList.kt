package eu.kanade.tachiyomi.ui.browse.feed.add

import eu.kanade.domain.source.interactor.SourceListUiMapper
import eu.kanade.presentation.browse.SourceUiModel
import tachiyomi.domain.source.model.Source

/**
 * The add-feed source list, sectioned like the Sources tab. While searching, the last-used entry is left out so
 * each matching source is listed once.
 */
internal fun addFeedSourceList(sources: List<Source>, query: String): List<SourceUiModel> {
    val trimmed = query.trim()
    val listed = if (trimmed.isEmpty()) {
        sources
    } else {
        sources.filter { !it.isUsedLast && it.name.contains(trimmed, ignoreCase = true) }
    }
    return SourceListUiMapper.map(listed).items
}
