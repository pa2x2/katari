package mihon.entry.interactions.viewer

/** Direction of travel through an ordered entry-child sequence. */
enum class EntryChildDirection {
    PREVIOUS,
    NEXT,
}

/**
 * The active child and its immediate neighbors in a viewer session.
 *
 * The payload is owned by the viewer implementation: manga uses reader chapters, BOOK readers
 * can use prepared resources, and anime can use stored episode metadata.
 */
data class EntryChildWindow<T>(
    val current: T,
    val previous: T? = null,
    val next: T? = null,
) {
    fun adjacent(direction: EntryChildDirection): T? = when (direction) {
        EntryChildDirection.PREVIOUS -> previous
        EntryChildDirection.NEXT -> next
    }

    fun previousTransition(): EntryChildTransition.Prev<T> = EntryChildTransition.Prev(current, previous)

    fun nextTransition(): EntryChildTransition.Next<T> = EntryChildTransition.Next(current, next)

    fun transition(direction: EntryChildDirection): EntryChildTransition<T> = when (direction) {
        EntryChildDirection.PREVIOUS -> previousTransition()
        EntryChildDirection.NEXT -> nextTransition()
    }
}

/**
 * A boundary between two entry children.
 *
 * A populated boundary keeps the same identity when crossed: NEXT(A, B) and PREVIOUS(B, A)
 * represent the same boundary. Terminal boundaries remain direction-sensitive so an isolated
 * child can expose distinct "no previous" and "no next" transitions.
 */
sealed class EntryChildTransition<T> protected constructor(
    val direction: EntryChildDirection,
    open val from: T,
    open val to: T?,
) {
    class Prev<T>(
        override val from: T,
        override val to: T?,
    ) : EntryChildTransition<T>(EntryChildDirection.PREVIOUS, from, to)

    class Next<T>(
        override val from: T,
        override val to: T?,
    ) : EntryChildTransition<T>(EntryChildDirection.NEXT, from, to)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntryChildTransition<*>) return false

        if (to == null || other.to == null) {
            return direction == other.direction && from == other.from && to == other.to
        }
        return (from == other.from && to == other.to) || (from == other.to && to == other.from)
    }

    override fun hashCode(): Int {
        if (to == null) return 31 * direction.hashCode() + from.hashCode()
        return from.hashCode() xor to.hashCode()
    }

    override fun toString(): String = "${javaClass.simpleName}(from=$from, to=$to)"
}

/** Builds the immediate viewer window around [current], or returns `null` when it is absent. */
fun <T> List<T>.entryChildWindow(current: T): EntryChildWindow<T>? {
    val index = indexOf(current)
    return entryChildWindowAt(index)
}

/** Builds the immediate viewer window around the item identified by [currentKey]. */
fun <T, K> List<T>.entryChildWindow(currentKey: K, keySelector: (T) -> K): EntryChildWindow<T>? {
    val index = indexOfFirst { keySelector(it) == currentKey }
    return entryChildWindowAt(index)
}

private fun <T> List<T>.entryChildWindowAt(index: Int): EntryChildWindow<T>? {
    if (index !in indices) return null
    return EntryChildWindow(
        current = this[index],
        previous = getOrNull(index - 1),
        next = getOrNull(index + 1),
    )
}
