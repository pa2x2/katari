package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.filter.withProjectedState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Own pending edits for the browsing session, including when the filter sheet is closed. */
internal class CatalogPagedFilterEdits(
    private val scope: CoroutineScope,
    private val onPendingChanged: (Int) -> Unit,
) {
    private val jobs = mutableMapOf<Job, EntryFilter.PagedGroup<*>>()

    fun submit(
        group: EntryFilter.PagedGroup<*>,
        item: EntryFilterPageItem,
        updated: EntryFilter<*>,
        onComplete: (Boolean) -> Unit,
    ) {
        // Supported projected leaves have immutable scalar state. Freeze the click before waiting for a source request.
        val value = updated.state
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val encoded = group.withProjectedState { source ->
                    val leaf = source.projectItem(item, null)
                    assignLeafValue(leaf, value)
                    source.applyItemUpdate(item, leaf)
                    source.encodeCurrentState()
                }
                ensureActive()
                onComplete(group.restoreEncodedState(encoded))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onComplete(false)
            }
        }
        synchronized(jobs) {
            jobs[job] = group
            onPendingChanged(jobs.size)
        }
        job.invokeOnCompletion {
            synchronized(jobs) {
                jobs.remove(job)
                onPendingChanged(jobs.size)
            }
        }
        job.start()
    }

    fun discard(target: EntryFilter<*>? = null) {
        fun contains(filter: EntryFilter<*>, group: EntryFilter.PagedGroup<*>): Boolean =
            filter === group || (filter as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>()
                ?.any { contains(it, group) } == true
        val discarded = synchronized(jobs) {
            jobs.filterValues { target == null || contains(target, it) }.keys.toList()
        }
        discarded.forEach { it.cancel() }
    }
}

@Suppress("UNCHECKED_CAST")
private fun assignLeafValue(filter: EntryFilter<*>, value: Any?) {
    (filter as EntryFilter<Any?>).state = value
}
