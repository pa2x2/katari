package tachiyomi.domain.entry.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository

/**
 * The expanded members of a merged chapter list shown while [scope] lives, kept in step with the stored choices of the
 * entry's [memberIds]. Toggles are stored one at a time in the order they were made, so quick taps end on the last one.
 */
class MergedMemberExpansionSession(
    private val repository: MergedMemberExpansionRepository,
    scope: CoroutineScope,
    memberIds: Flow<List<Long>>,
) {
    private val mutableState = MutableStateFlow(MergedMemberExpansion())
    val state: StateFlow<MergedMemberExpansion> = mutableState.asStateFlow()

    private val writes = Channel<Pair<Long, Boolean>>(Channel.UNLIMITED)

    init {
        scope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            memberIds
                .map { ids -> ids.takeIf { it.size > 1 }.orEmpty() }
                .distinctUntilChanged()
                .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptySet()) else repository.subscribeExpanded(ids) }
                .collect { saved -> mutableState.update { it.withSaved(saved) } }
        }
        scope.launch {
            for ((memberId, expanded) in writes) repository.setExpanded(memberId, expanded)
        }
    }

    fun toggle(memberId: Long, currentMemberId: Long?) {
        val expansion = mutableState.updateAndGet { it.toggle(memberId, currentMemberId) }
        writes.trySend(memberId to (memberId in expansion.shown(currentMemberId)))
    }
}
