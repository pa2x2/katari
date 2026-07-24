package eu.kanade.tachiyomi.ui.browse.immersive

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.entry.interactions.EntryImmersiveAvailability
import mihon.entry.interactions.EntryImmersiveChildRefreshResult
import mihon.entry.interactions.EntryImmersiveChildRequirement
import mihon.entry.interactions.EntryImmersiveContext
import mihon.entry.interactions.EntryImmersiveFeature
import mihon.entry.interactions.EntryImmersiveHandle
import mihon.entry.interactions.EntryImmersiveLoadRequest
import mihon.entry.interactions.EntryImmersiveLoadResult
import mihon.entry.interactions.EntryImmersiveProgress
import mihon.entry.interactions.EntryImmersiveUnavailableReason
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

class EntryImmersiveScreenModel(
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val immersiveFeature: EntryImmersiveFeature = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<EntryImmersiveScreenModel.State>(State()) {

    private val loadJobs = ConcurrentHashMap<EntryImmersiveItemKey, Job>()
    private val loadTokens = ConcurrentHashMap<EntryImmersiveItemKey, Any>()

    fun load(context: Context, entry: Entry, force: Boolean = false) {
        val ref = entry.immersiveItemKey()
        val existing = state.value.items[ref]
        if (!force && (existing is ItemState.Loading || existing is ItemState.Ready)) return

        loadJobs.remove(ref)?.cancel()
        release(existing)

        val token = Any()
        loadTokens[ref] = token
        mutableState.update { current ->
            current.copy(items = current.items + (ref to ItemState.Loading(entry)))
        }

        val job = screenModelScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val nextState = resolveItem(context, entry, forceSync = force)
                val accepted = synchronized(loadTokens) {
                    if (loadTokens[ref] !== token) {
                        false
                    } else {
                        mutableState.update { current ->
                            current.copy(items = current.items + (ref to nextState))
                        }
                        true
                    }
                }
                if (!accepted) release(nextState)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                synchronized(loadTokens) {
                    if (loadTokens[ref] === token) {
                        mutableState.update { current ->
                            current.copy(items = current.items + (ref to ItemState.Error(entry, e)))
                        }
                    }
                }
            } finally {
                synchronized(loadTokens) {
                    if (loadTokens[ref] === token) {
                        loadTokens.remove(ref)
                        loadJobs.remove(ref)
                    }
                }
            }
        }
        loadJobs[ref] = job
        job.start()
    }

    fun retain(itemRefs: Set<EntryImmersiveItemKey>) {
        val evictedRefs = (loadJobs.keys + state.value.items.keys).filterNot(itemRefs::contains).toSet()
        if (evictedRefs.isEmpty()) return

        val releasedStates = synchronized(loadTokens) {
            evictedRefs.forEach { ref ->
                loadJobs.remove(ref)?.cancel()
                loadTokens.remove(ref)
            }
            val states = evictedRefs.mapNotNull(state.value.items::get)
            mutableState.update { current ->
                current.copy(items = current.items.filterKeys { it in itemRefs })
            }
            states
        }
        releasedStates.forEach(::release)
    }

    fun retry(context: Context, entry: Entry) {
        load(context, entry, force = true)
    }

    fun renderer(handle: EntryImmersiveHandle) = immersiveFeature.renderer(handle)

    fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) {
        screenModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                immersiveFeature.persistProgress(handle, progress)
            }
        }
    }

    override fun onDispose() {
        loadJobs.values.forEach(Job::cancel)
        loadJobs.clear()
        loadTokens.clear()
        state.value.items.values.forEach(::release)
        super.onDispose()
    }

    private suspend fun resolveItem(context: Context, entry: Entry, forceSync: Boolean): ItemState {
        val source = sourceManager.get(entry.source)
        val availability = when (
            val result = immersiveFeature.availability(EntryImmersiveContext(entry, source))
        ) {
            is EntryImmersiveAvailability.Available -> result
            is EntryImmersiveAvailability.ContextuallyUnavailable ->
                return ItemState.Unavailable(entry, result.reason)
            is EntryImmersiveAvailability.Inapplicable -> return ItemState.Inapplicable(entry)
        }

        val chapters = when (availability.childRequirement) {
            EntryImmersiveChildRequirement.NONE -> emptyList()
            EntryImmersiveChildRequirement.FIRST_READING_CHILD -> {
                var current = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                if (forceSync || current.isEmpty()) {
                    when (val refresh = immersiveFeature.refreshChildren(entry)) {
                        EntryImmersiveChildRefreshResult.Refreshed -> Unit
                        is EntryImmersiveChildRefreshResult.ContextuallyUnavailable -> {
                            return ItemState.Unavailable(entry, refresh.reason)
                        }
                        is EntryImmersiveChildRefreshResult.Inapplicable -> return ItemState.Inapplicable(entry)
                        is EntryImmersiveChildRefreshResult.Failed -> return ItemState.Error(entry, refresh.error)
                    }
                    current = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                }
                current
            }
        }

        return when (
            val result = immersiveFeature.load(
                EntryImmersiveLoadRequest(
                    context = context,
                    entry = entry,
                    source = source,
                    children = chapters,
                ),
            )
        ) {
            is EntryImmersiveLoadResult.Loaded -> ItemState.Ready(entry, result.child, result.handle)
            is EntryImmersiveLoadResult.ContextuallyUnavailable -> ItemState.Unavailable(entry, result.reason)
            is EntryImmersiveLoadResult.Inapplicable -> ItemState.Inapplicable(entry)
            is EntryImmersiveLoadResult.Failed -> ItemState.Error(entry, result.error)
        }
    }

    private fun release(state: ItemState?) {
        val ready = state as? ItemState.Ready ?: return
        immersiveFeature.release(ready.handle)
    }

    data class State(
        val items: Map<EntryImmersiveItemKey, ItemState> = emptyMap(),
    )

    sealed interface ItemState {
        val entry: Entry

        data class Loading(override val entry: Entry) : ItemState

        data class Ready(
            override val entry: Entry,
            val chapter: EntryChapter?,
            val handle: EntryImmersiveHandle,
        ) : ItemState

        data class Inapplicable(override val entry: Entry) : ItemState

        data class Unavailable(
            override val entry: Entry,
            val reason: EntryImmersiveUnavailableReason,
        ) : ItemState

        data class Error(
            override val entry: Entry,
            val throwable: Throwable,
        ) : ItemState
    }
}

data class EntryImmersiveItemKey(
    val id: Long,
    val type: EntryType,
)

fun Entry.immersiveItemKey(): EntryImmersiveItemKey {
    return EntryImmersiveItemKey(id = id, type = type)
}

fun entryImmersiveItemKey(key: EntryImmersiveItemKey): String {
    return "${key.type.name}:${key.id}"
}
