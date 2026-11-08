package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * Defines how a library entry should be considered during updates.
 */
@Serializable
enum class EntryUpdateStrategy {
    ALWAYS_UPDATE,
    ONLY_FETCH_ONCE,
}
