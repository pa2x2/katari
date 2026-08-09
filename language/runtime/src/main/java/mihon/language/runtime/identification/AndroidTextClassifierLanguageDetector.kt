package mihon.language.runtime.identification

import android.app.Application
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageDetectorId
import mihon.language.api.tag.LanguageTag

class AndroidTextClassifierLanguageDetector(
    private val classify: (String) -> LanguageCandidate?,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TextLanguageDetector {
    override val id = TextLanguageDetectorId("android-text-classifier")

    override suspend fun detect(text: String): TextLanguageDetection {
        return try {
            val candidate = withContext(workerDispatcher) { classify(text) }
                ?: return TextLanguageDetection.Undetermined
            if (candidate.confidence < MINIMUM_CONFIDENCE) {
                return TextLanguageDetection.Undetermined
            }
            val language = LanguageTag.parse(candidate.languageTag)
                ?: return TextLanguageDetection.Undetermined
            TextLanguageDetection.Detected(language, candidate.confidence)
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            TextLanguageDetection.Unavailable("Android language identification failed")
        }
    }

    data class LanguageCandidate(
        val languageTag: String,
        val confidence: Float,
    )

    private companion object {
        const val MINIMUM_CONFIDENCE = 0.5f
    }
}

fun androidTextClassifierLanguageDetector(
    application: Application,
    sdkInt: Int = Build.VERSION.SDK_INT,
): TextLanguageDetector? {
    if (sdkInt < Build.VERSION_CODES.Q) return null

    val textClassifier = application
        .getSystemService(TextClassificationManager::class.java)
        ?.textClassifier
        ?: return null
    return AndroidTextClassifierLanguageDetector(
        classify = { text ->
            val result = textClassifier.detectLanguage(
                TextLanguage.Request.Builder(text).build(),
            )
            if (result.localeHypothesisCount == 0) {
                null
            } else {
                val locale = result.getLocale(0)
                AndroidTextClassifierLanguageDetector.LanguageCandidate(
                    languageTag = locale.toLanguageTag(),
                    confidence = result.getConfidenceScore(locale),
                )
            }
        },
    )
}
