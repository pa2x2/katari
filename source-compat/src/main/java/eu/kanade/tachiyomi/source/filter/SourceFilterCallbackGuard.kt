package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterStateSemantics
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Filters whose extension callbacks failed while the host rendered, counted, or validated them.
 *
 * Source filters compare by name and state, so failures are tracked by instance identity.
 */
internal class SourceFilterCallbackFailures {
    private val failed: MutableSet<EntryFilter<*>> =
        Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

    fun record(source: EntryFilter<*>) {
        failed += source
    }

    operator fun contains(source: EntryFilter<*>): Boolean = source in failed
}

/**
 * Runs a synchronous extension filter callback that the host needs while editing.
 *
 * A failing extension degrades only the affected filter: the host continues with [fallback] and can report the
 * failure through [hasFailedSourceCallback] instead of crashing the whole editor.
 */
internal inline fun <T> EntryFilter<*>.guardSourceCallback(
    binding: EntryFilterBinding,
    fallback: T,
    block: () -> T,
): T = try {
    block()
} catch (error: Exception) {
    recordSourceCallbackFailure(binding, error)
    fallback
} catch (error: LinkageError) {
    recordSourceCallbackFailure(binding, error)
    fallback
}

internal fun EntryFilter<*>.recordSourceCallbackFailure(binding: EntryFilterBinding, error: Throwable) {
    binding.callbackFailures.record(this)
    logcat(LogPriority.ERROR, error) { "Source filter callback failed for \"$name\"" }
}

/** Whether one of this filter's extension callbacks failed while the host used it. */
fun EntryFilter<*>.hasFailedSourceCallback(): Boolean {
    val detached = this as? DetachedEntryFilter ?: return false
    return detached.sourceFilter in detached.binding.callbackFailures
}

/** Resolve optional semantics through a host projection without requiring old extensions to implement anything. */
fun EntryFilter<*>.sourceStateSemantics(): EntryFilterStateSemantics? {
    val detached = this as? DetachedEntryFilter ?: return this as? EntryFilterStateSemantics
    val semantics = detached.sourceFilter as? EntryFilterStateSemantics ?: return null
    return GuardedStateSemantics(detached.sourceFilter, semantics, detached.binding)
}

private class GuardedStateSemantics(
    private val source: EntryFilter<*>,
    private val semantics: EntryFilterStateSemantics,
    private val binding: EntryFilterBinding,
) : EntryFilterStateSemantics {
    override fun activeSelectionCount(value: EntryFilter<*>): Int? =
        source.guardSourceCallback(binding, null) { semantics.activeSelectionCount(value) }

    override val canClearSelection: Boolean
        get() = source.guardSourceCallback(binding, false) { semantics.canClearSelection }

    override fun clearSelection(value: EntryFilter<*>) {
        source.guardSourceCallback(binding, Unit) { semantics.clearSelection(value) }
    }
}
