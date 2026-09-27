package mihon.entry.interactions.translation

import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.Entry

/** Portable form of a series' translation languages, as BCP-47 tags, for backups. */
@Serializable
data class EntryTranslationLanguagesSnapshot(
    val contentLanguage: String? = null,
    val targetLanguage: String? = null,
    val updatedAt: Long = 0L,
)

/** The target language captured from the Migration source, so durable retry never rereads it. */
@Serializable
data class EntryTranslationLanguagesMigrationPayload(
    val target: Entry,
    val targetLanguage: String,
)
