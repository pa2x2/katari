package tachiyomi.domain.entry.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * The open sections of a chapter list shown while [scope] lives, kept in step with the [saved] choices. Toggles are
 * passed to [store] one at a time in the order they were made, so quick taps end on the last one.
 */
class SectionExpansionSession<K>(
    scope: CoroutineScope,
    saved: Flow<Set<K>>,
    private val store: suspend (section: K, expanded: Boolean) -> Unit,
) {
    private val mutableState = MutableStateFlow(SectionExpansion<K>())
    val state: StateFlow<SectionExpansion<K>> = mutableState.asStateFlow()

    private val writes = Channel<Pair<K, Boolean>>(Channel.UNLIMITED)

    init {
        scope.launch {
            saved.collect { sections -> mutableState.update { it.withSaved(sections) } }
        }
        scope.launch {
            for ((section, expanded) in writes) store(section, expanded)
        }
    }

    fun toggle(section: K, current: K?) {
        val expansion = mutableState.updateAndGet { it.toggle(section, current) }
        writes.trySend(section to (section in expansion.shown(current)))
    }
}
