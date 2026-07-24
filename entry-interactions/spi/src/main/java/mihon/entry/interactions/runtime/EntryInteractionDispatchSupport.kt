package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.model.Entry

internal fun <T> Map<EntryType, T>.requireProcessor(category: String, type: EntryType): T {
    return this[type] ?: error(
        "No $category processor registered for EntryType $type. Registered types: ${registeredTypes()}",
    )
}

internal fun EntryInteractionProvider.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryOpenProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryContinueProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryDownloadProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryConsumptionProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryBookmarkProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryPlaybackPreferencesProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryProgressProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryImmersiveProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryChildListProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun EntryChildGroupFilterProcessor.requireMatchingEntryType(
    category: String,
    entry: Entry,
    registeredTypes: Set<EntryType>,
) {
    require(type == entry.type) {
        processorMismatchMessage(category, entry.type, type, registeredTypes)
    }
}

internal fun processorMismatchMessage(
    category: String,
    requestedType: EntryType,
    processorType: EntryType,
    registeredTypes: Set<EntryType>,
): String {
    return "Mismatched $category processor for EntryType $requestedType: processor type was $processorType. " +
        "Registered types: ${registeredTypes.registeredTypes()}"
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
