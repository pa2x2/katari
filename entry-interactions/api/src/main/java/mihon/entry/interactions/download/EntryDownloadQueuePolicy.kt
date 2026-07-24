package mihon.entry.interactions

/** Shared ordering rules for entry download queues. */
object EntryDownloadQueuePolicy {

    /**
     * Applies a requested pending order while keeping active work ahead of it and free from removal.
     */
    fun <T, K> reorderPending(
        queue: List<T>,
        requested: List<T>,
        keyOf: (T) -> K,
        isActive: (T) -> Boolean,
    ): List<T> {
        val active = queue.filter(isActive)
        if (active.isEmpty()) return requested
        val activeKeys = active.mapTo(mutableSetOf(), keyOf)
        return active + requested.filterNot { keyOf(it) in activeKeys }
    }

    /**
     * Moves matching items ahead of other pending work while retaining their existing relative order.
     */
    fun <T, K> promote(
        queue: List<T>,
        keys: Collection<K>,
        keyOf: (T) -> K,
        isActive: (T) -> Boolean = { false },
    ): List<T> {
        if (queue.isEmpty() || keys.isEmpty()) return queue
        val priorityKeys = keys.toSet()
        val (active, pending) = queue.partition(isActive)
        val (priority, remaining) = pending.partition { keyOf(it) in priorityKeys }
        return active + priority + remaining
    }
}
