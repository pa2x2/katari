package mihon.translation.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.spi.TranslationSourceLanguageDetection
import mihon.translation.spi.TranslationSourceLanguageDetector
import mihon.translation.spi.TranslationSourceLanguageDetectorId

internal class MlKitSourceLanguageDetector(
    private val identifyLanguage: suspend (String) -> String,
) : TranslationSourceLanguageDetector {
    override val id = TranslationSourceLanguageDetectorId("mlkit-language-id")

    override suspend fun detect(text: String): TranslationSourceLanguageDetection {
        return try {
            val tag = identifyLanguage(text)
            val language = TranslationLanguageTag.parse(tag)
                ?: return TranslationSourceLanguageDetection.Undetermined
            TranslationSourceLanguageDetection.Detected(language)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationSourceLanguageDetection.Unavailable("ML Kit language identification failed")
        }
    }
}

internal fun createMlKitSourceLanguageDetector(): TranslationSourceLanguageDetector {
    return MlKitSourceLanguageDetector { text ->
        val identifier = LanguageIdentification.getClient()
        try {
            identifier.identifyLanguage(text).await()
        } finally {
            identifier.close()
        }
    }
}
