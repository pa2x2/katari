package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.library.model.LibraryItemKey

/**
 * Random sort ranks every entry by a hash of the seed and its key instead of shuffling the list. A seeded shuffle
 * only repeats for the same input, so an entry leaving the page (such as one a filter hides once it is read) would
 * reshuffle every other entry and throw off the scroll position.
 */
internal fun List<LibraryItemKey>.inRandomOrder(seed: Int): List<LibraryItemKey> {
    return sortedBy { key -> randomRank(seed, key) }
}

private fun randomRank(seed: Int, key: LibraryItemKey): Long {
    val identity = mix(key.id * GOLDEN_GAMMA + key.type.ordinal)
    return mix(identity xor seed.toLong())
}

// SplitMix64 finalizer: spreads neighbouring ids and seeds across the whole range.
private fun mix(value: Long): Long {
    var z = value
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}

private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
