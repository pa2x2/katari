package mihon.translation.runtime

import android.app.Application
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.spi.TranslationSourceLanguageDetection
import mihon.translation.spi.TranslationSourceLanguageDetector
import mihon.translation.spi.TranslationSourceLanguageDetectorId

internal class AndroidTextClassifierSourceLanguageDetector(
    private val classify: (String) -> LanguageCandidate?,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TranslationSourceLanguageDetector {
    override val id = TranslationSourceLanguageDetectorId("android-text-classifier")

    override suspend fun detect(text: String): TranslationSourceLanguageDetection {
        return try {
            val candidate = withContext(workerDispatcher) { classify(text) }
                ?: return TranslationSourceLanguageDetection.Undetermined
            val language = TranslationLanguageTag.parse(candidate.languageTag)
                ?: return TranslationSourceLanguageDetection.Undetermined
            TranslationSourceLanguageDetection.Detected(language, candidate.confidence)
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            TranslationSourceLanguageDetection.Unavailable("Android language identification failed")
        }
    }

    internal data class LanguageCandidate(
        val languageTag: String,
        val confidence: Float,
    )
}

internal fun androidTextClassifierSourceLanguageDetector(
    application: Application,
    sdkInt: Int = Build.VERSION.SDK_INT,
): TranslationSourceLanguageDetector? {
    if (sdkInt < Build.VERSION_CODES.Q) return null

    val textClassifier = application
        .getSystemService(TextClassificationManager::class.java)
        ?.textClassifier
        ?: return null
    return AndroidTextClassifierSourceLanguageDetector(
        classify = { text ->
            val result = textClassifier.detectLanguage(
                TextLanguage.Request.Builder(text).build(),
            )
            if (result.localeHypothesisCount == 0) {
                null
            } else {
                val locale = result.getLocale(0)
                AndroidTextClassifierSourceLanguageDetector.LanguageCandidate(
                    languageTag = locale.toLanguageTag(),
                    confidence = result.getConfidenceScore(locale),
                )
            }
        },
    )
}
