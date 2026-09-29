package mihon.entry.interactions.translate.queue

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.entry.interactions.translate.EntryTranslateSetup
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.translation.api.engine.TranslationEngineId

/** Encodes the setup a queued chapter was queued with, as the queue stores it until the chapter is translated. */
internal object EntryTranslateSetupCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(setup: EntryTranslateSetup): String = json.encodeToString(
        StoredSetup(
            contentLanguage = setup.contentLanguage.value,
            targetLanguage = setup.targetLanguage.value,
            engine = setup.engine.value,
            recognition = setup.recognition?.components?.map { it.value }.orEmpty(),
        ),
    )

    /** The stored setup, or `null` when it cannot be read back, for example a language that no longer parses. */
    fun decode(value: String): EntryTranslateSetup? = try {
        val stored = json.decodeFromString<StoredSetup>(value)
        EntryTranslateSetup(
            contentLanguage = LanguageTag.parse(stored.contentLanguage) ?: return null,
            targetLanguage = LanguageTag.parse(stored.targetLanguage) ?: return null,
            engine = TranslationEngineId(stored.engine),
            recognition = when (stored.recognition.size) {
                0 -> null
                2 -> TextRecognitionPipeline(
                    detector = TextRecognitionComponentId(stored.recognition[0]),
                    recognizer = TextRecognitionComponentId(stored.recognition[1]),
                )
                else -> return null
            },
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
