package mihon.entry.interactions.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.EntryDownloadQueueGroup

internal fun <T> Map<EntryType, T>.requireProcessor(category: String, type: EntryType): T {
    return this[type] ?: error(
        "No $category processor registered for EntryType $type. Registered types: ${registeredTypes()}",
    )
}

internal fun Map<EntryType, *>.registeredTypes(): String {
    return keys.registeredTypes()
}

internal fun Set<EntryType>.registeredTypes(): String {
    return sortedBy { it.name }.joinToString().ifEmpty { "none" }
}

internal fun <T> List<Flow<T>>.merged(): Flow<T> {
    return when (size) {
        0 -> emptyFlow()
        1 -> first()
        else -> channelFlow {
            forEach { flow ->
                launch {
                    flow.collect { send(it) }
                }
            }
        }
    }
}

/** Emits after every input has a current value, then whenever any input changes. */
internal fun List<Flow<Unit>>.combinedLatestUnit(): Flow<Unit> {
    return when (size) {
        0 -> flowOf(Unit)
        1 -> first()
        else -> channelFlow {
            val ready = BooleanArray(size)
            forEachIndexed { index, flow ->
                launch {
                    flow.collect {
                        val allReady = synchronized(ready) {
                            ready[index] = true
                            ready.all { it }
                        }
                        if (allReady) send(Unit)
                    }
                }
            }
        }
    }
}

internal fun List<Flow<Boolean>>.combinedAny(): Flow<Boolean> {
    return when (size) {
        0 -> flowOf(false)
        1 -> first()
        else -> combine(this) { values -> values.any { it } }
    }
}

internal fun List<Flow<List<EntryDownloadQueueGroup>>>.combinedFlatten(): Flow<List<EntryDownloadQueueGroup>> {
    return when (size) {
        0 -> flowOf(emptyList())
        1 -> first()
        else -> combine(this) { values -> values.flatMap { it } }
    }
}
