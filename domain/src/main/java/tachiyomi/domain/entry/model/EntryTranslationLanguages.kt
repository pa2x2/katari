package tachiyomi.domain.entry.model

/**
 * Translation languages chosen for one entry, as BCP-47 tags, and whether its downloads are translated. A record
 * chooses at least one of them; a missing language follows the source's declared language or the profile's target.
 */
data class EntryTranslationLanguages(
    val entryId: Long,
    val contentLanguage: String?,
    val targetLanguage: String?,
    val updatedAt: Long,
    val translateDownloads: Boolean = false,
)
