package tachiyomi.domain.library.model

import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * Composite identity for a library entry. Required because manga and anime IDs are
 * independent SQLite sequences and can collide.
 */
data class LibraryItemKey(
    val type: EntryType,
    val id: Long,
) {
    override fun toString(): String = "${type.name}:$id"
}
