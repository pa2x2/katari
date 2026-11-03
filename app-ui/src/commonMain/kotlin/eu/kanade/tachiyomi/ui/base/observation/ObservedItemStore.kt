package eu.kanade.tachiyomi.ui.base.observation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Live state for the items of a scrolling list, observed through one batched subscription.
 *
 * Subscribing per composed item makes every database write re-run one query per visible item
 * and leaves freshly composed items empty until their own query returns. Instead, items already
 * loaded by the screen are seeded here so they render immediately, and every requested key
 * joins a single shared observation.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ObservedItemStore<K : Any, T : Any>(
    scope: CoroutineScope,
    private val keyOf: (T) -> K,
    private val supersedes: (candidate: T, current: T) -> Boolean,
    observeItems: suspend (List<K>) -> Flow<List<T>>,
) {
    private val states = ConcurrentHashMap<K, MutableStateFlow<T?>>()
    private val registrationGeneration = MutableStateFlow(0L)

    init {
        registrationGeneration
            .debounce(REGISTRATION_DEBOUNCE_MILLIS)
            .map { states.keys.toSet() }
            .distinctUntilChanged()
            .flatMapLatest { keys -> if (keys.isEmpty()) emptyFlow() else observeItems(keys.toList()) }
            .onEach { items -> items.forEach(::publish) }
            .launchIn(scope)
    }

    /** Returns the observed state of [key], which stays `null` until the item is seeded or observed. */
    fun stateOf(key: K): StateFlow<T?> {
        return register(key)
    }

    /** Seeds [item] as the latest known value and returns its observed state. */
    fun seed(item: T): StateFlow<T?> {
        return register(keyOf(item)).also { publish(item) }
    }

    fun seedAll(items: Iterable<T>) {
        items.forEach(::seed)
    }

    private fun register(key: K): MutableStateFlow<T?> {
        states[key]?.let { return it }
        val state = MutableStateFlow<T?>(null)
        val existing = states.putIfAbsent(key, state)
        if (existing != null) return existing
        registrationGeneration.update(Long::inc)
        return state
    }

    private fun publish(item: T) {
        val state = states[keyOf(item)] ?: return
        state.update { current -> if (current == null || supersedes(item, current)) item else current }
    }

    private companion object {
        const val REGISTRATION_DEBOUNCE_MILLIS = 50L
    }
}
