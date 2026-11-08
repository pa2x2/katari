package tachiyomi.domain.entry.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.repository.ChapterGroupExpansionRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository

/** The open members of the merged chapter list of [memberIds]; an entry that isn't merged has none. */
@OptIn(ExperimentalCoroutinesApi::class)
fun MergedMemberExpansionRepository.expansionSession(
    scope: CoroutineScope,
    memberIds: Flow<List<Long>>,
): SectionExpansionSession<Long> = SectionExpansionSession(
    scope = scope,
    saved = memberIds
        .map { ids -> ids.takeIf { it.size > 1 }.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptySet()) else subscribeExpanded(ids) },
    store = ::setExpanded,
)

/** The open groups of chapters of [entryIds], the entry itself or each member of a merge. */
@OptIn(ExperimentalCoroutinesApi::class)
fun ChapterGroupExpansionRepository.expansionSession(
    scope: CoroutineScope,
    entryIds: Flow<List<Long>>,
): SectionExpansionSession<ChapterGroupId> = SectionExpansionSession(
    scope = scope,
    saved = entryIds
        .distinctUntilChanged()
        .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptySet()) else subscribeExpanded(ids) },
    store = ::setExpanded,
)
