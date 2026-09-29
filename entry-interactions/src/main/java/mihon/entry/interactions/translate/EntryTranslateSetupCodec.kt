package mihon.entry.interactions.translate

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.language.api.tag.LanguageTag

/** Encodes the setup a queued chapter was queued with, as the queue stores it until the chapter is translated. */
internal object EntryTranslateSetupCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(setup: EntryTranslateSetup): String = json.encodeToString(
        StoredSetup(
            contentLanguage = setup.contentLanguage.value,
            targetLanguage = setup.targetLanguage.value,
            engine = setup.engine,
            recognition = setup.recognition,
        ),
    )

    /** The stored setup, or `null` when it cannot be read back, for example a language that no longer parses. */
    fun decode(value: String): EntryTranslateSetup? = try {
        val stored = json.decodeFromString<StoredSetup>(value)
        EntryTranslateSetup(
            contentLanguage = LanguageTag.parse(stored.contentLanguage) ?: return null,
            targetLanguage = LanguageTag.parse(stored.targetLanguage) ?: return null,
            engine = stored.engine,
            recognition = stored.recognition,
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    @Serializable
    private class StoredSetup(
        val contentLanguage: String,
        val targetLanguage: String,
        val engine: String,
        val recognition: List<String> = emptyList(),
    )
}
