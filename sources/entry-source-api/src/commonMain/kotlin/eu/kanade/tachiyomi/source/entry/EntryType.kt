package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * Discriminator for content returned by a [UnifiedSource].
 *
 * Sources are type-agnostic and may return items of any type in a single list.
 */
@Serializable
enum class EntryType {
    MANGA,
    ANIME,
    BOOK,
}
