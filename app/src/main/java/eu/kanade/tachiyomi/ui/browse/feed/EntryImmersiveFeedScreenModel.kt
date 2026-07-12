package eu.kanade.tachiyomi.ui.browse.feed

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.model.FeedItemRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.entry.interactions.EntryChildListInteraction
import mihon.entry.interactions.EntryImmersiveFeedHandle
import mihon.entry.interactions.EntryImmersiveFeedInteraction
import mihon.entry.interactions.EntryImmersiveFeedProgress
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

class EntryImmersiveFeedScreenModel(
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val syncEntryWithSource: SyncEntryWithSource = Injekt.get(),
    private val childListInteraction: EntryChildListInteraction = Injekt.get(),
    private val immersiveFeedInteraction: EntryImmersiveFeedInteraction = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<EntryImmersiveFeedScreenModel.State>(State()) {

    private val loadJobs = ConcurrentHashMap<FeedItemRef, Job>()
    private val loadTokens = ConcurrentHashMap<FeedItemRef, Any>()

    fun load(context: Context, entry: Entry, force: Boolean = false) {
        val ref = FeedItemRef(entry.id, entry.type)
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

    fun retain(itemRefs: Set<FeedItemRef>) {
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

    fun renderer(handle: EntryImmersiveFeedHandle) = immersiveFeedInteraction.renderer(handle)

    fun persistProgress(handle: EntryImmersiveFeedHandle, progress: EntryImmersiveFeedProgress) {
        screenModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                immersiveFeedInteraction.persistProgress(handle, progress)
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
        if (!immersiveFeedInteraction.isSupported(entry)) {
            error("Immersive feed is not supported for this entry type")
        }

        var chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        if (forceSync || chapters.isEmpty()) {
            syncEntryWithSource(entry, fetchDetails = false, fetchChapters = true)
            chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        }

        val chapter = childListInteraction.sortedForReading(entry, chapters, emptyList()).firstOrNull()
            ?: error("No consumable item found")
        val source = sourceManager.get(entry.source) ?: error("Source not available")
        val handle = immersiveFeedInteraction.load(context, entry, chapter, source)
        return ItemState.Ready(entry, chapter, handle)
    }

    private fun release(state: ItemState?) {
        val ready = state as? ItemState.Ready ?: return
        immersiveFeedInteraction.release(ready.handle)
    }

    data class State(
        val items: Map<FeedItemRef, ItemState> = emptyMap(),
    )

    sealed interface ItemState {
        val entry: Entry

        data class Loading(override val entry: Entry) : ItemState

        data class Ready(
            override val entry: Entry,
            val chapter: EntryChapter,
            val handle: EntryImmersiveFeedHandle,
        ) : ItemState

        data class Error(
            override val entry: Entry,
            val throwable: Throwable,
        ) : ItemState
    }
}
