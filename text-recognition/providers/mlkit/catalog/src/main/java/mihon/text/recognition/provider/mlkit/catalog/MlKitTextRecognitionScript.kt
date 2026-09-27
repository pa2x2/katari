package mihon.text.recognition.provider.mlkit.catalog

import mihon.language.api.tag.LanguageTag

/**
 * ML Kit text recognition models, one per script. Google Play services downloads each model on request.
 *
 * @property spaced whether words are separated by spaces, so lines of one region are joined with a space.
 */
enum class MlKitTextRecognitionScript(
    val displayName: String,
    languageCodes: List<String>,
    val spaced: Boolean = true,
) {
    Latin(
        displayName = "Latin script",
        languageCodes = listOf(
            "en", "fr", "de", "es", "it", "pt", "nl", "pl", "sv", "da", "no", "fi", "cs", "sk", "sl", "hr", "hu", "ro",
            "tr", "id", "ms", "vi", "tl", "ca", "et", "lv", "lt",
        ),
    ),
    Japanese(displayName = "Japanese", languageCodes = listOf("ja"), spaced = false),
    Chinese(displayName = "Chinese", languageCodes = listOf("zh"), spaced = false),
    Korean(displayName = "Korean", languageCodes = listOf("ko")),
    Devanagari(displayName = "Devanagari", languageCodes = listOf("hi", "mr", "ne", "sa")),
    ;

    val languages: Set<LanguageTag> = languageCodes.map(LanguageTag::require).toSet()

    companion object {
        fun forLanguage(language: LanguageTag): MlKitTextRecognitionScript? {
            val primary = language.value.substringBefore('-')
            return entries.firstOrNull { script -> script.languages.any { it.value == primary } }
        }
    }
}
