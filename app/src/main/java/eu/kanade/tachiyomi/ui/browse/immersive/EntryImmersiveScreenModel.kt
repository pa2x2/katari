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
import mihon.entry.interactions.EntryChildListInteraction
import mihon.entry.interactions.EntryImmersiveHandle
import mihon.entry.interactions.EntryImmersiveInteraction
import mihon.entry.interactions.EntryImmersiveProgress
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

class EntryImmersiveScreenModel(
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val syncEntryWithSource: SyncEntryWithSource = Injekt.get(),
    private val childListInteraction: EntryChildListInteraction = Injekt.get(),
    private val immersiveInteraction: EntryImmersiveInteraction = Injekt.get(),
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

    fun renderer(handle: EntryImmersiveHandle) = immersiveInteraction.renderer(handle)

    fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) {
        screenModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                immersiveInteraction.persistProgress(handle, progress)
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
        if (!immersiveInteraction.isSupported(entry)) {
            error("Immersive browsing is not supported for this entry type")
        }

        var chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        if (forceSync || chapters.isEmpty()) {
            syncEntryWithSource(entry, fetchDetails = false, fetchChapters = true)
            chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        }

        val chapter = childListInteraction.sortedForReading(entry, chapters, emptyList()).firstOrNull()
            ?: error("No consumable item found")
        val source = sourceManager.get(entry.source) ?: error("Source not available")
        val handle = immersiveInteraction.load(context, entry, chapter, source)
        return ItemState.Ready(entry, chapter, handle)
    }

    private fun release(state: ItemState?) {
        val ready = state as? ItemState.Ready ?: return
        immersiveInteraction.release(ready.handle)
    }

    data class State(
        val items: Map<EntryImmersiveItemKey, ItemState> = emptyMap(),
    )

    sealed interface ItemState {
        val entry: Entry

        data class Loading(override val entry: Entry) : ItemState

        data class Ready(
            override val entry: Entry,
            val chapter: EntryChapter,
            val handle: EntryImmersiveHandle,
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
