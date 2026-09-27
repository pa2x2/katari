package tachiyomi.domain.entry.model

/**
 * Translation languages chosen for one entry, as BCP-47 tags. At least one is set; a missing one follows the
 * source's declared language or the profile's target.
 */
data class EntryTranslationLanguages(
    val entryId: Long,
    val contentLanguage: String?,
    val targetLanguage: String?,
    val updatedAt: Long,
)
