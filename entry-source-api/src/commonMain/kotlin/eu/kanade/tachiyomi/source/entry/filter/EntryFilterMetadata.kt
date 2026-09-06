package eu.kanade.tachiyomi.source.entry.filter

/** Optional capabilities: existing extension filters need not implement this interface. */
interface EntryFilterMetadataProvider {
    val filterMetadata: EntryFilterMetadata
}

/**
 * Identity and presentation owned by a source, independently of translated labels and layout.
 * [id] must be unique across the source's filter tree and must never be reused for another meaning.
 * [optionIds] follow the current display order; their values must remain stable when that order changes.
 * [legacyOptionIds] explicitly describes indexes written before option identity was available.
 */
data class EntryFilterMetadata(
    val id: String? = null,
    val description: String? = null,
    val role: EntryFilterRole = EntryFilterRole.CONSTRAINT,
    val optionIds: List<String> = emptyList(),
    val aliases: Set<String> = emptySet(),
    val legacyNames: Set<String> = emptySet(),
    val legacyOptionIds: List<String> = emptyList(),
    val optionAliases: Map<String, String> = emptyMap(),
    val neutralOptionId: String? = null,
)

enum class EntryFilterRole { CONSTRAINT, ORDERING }
