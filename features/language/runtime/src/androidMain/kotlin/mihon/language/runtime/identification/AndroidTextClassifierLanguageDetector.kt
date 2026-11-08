package mihon.language.runtime.identification

import android.app.Application
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.language.api.identification.TextLanguageCandidate
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageDetectorId
import mihon.language.api.tag.LanguageTag

class AndroidTextClassifierLanguageDetector(
    private val classify: (String) -> List<LanguageCandidate>,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TextLanguageDetector {
    override val id = TextLanguageDetectorId("android-text-classifier")

    override suspend fun detect(text: String): TextLanguageDetection {
        return try {
            val candidates = withContext(workerDispatcher) { classify(text) }
                .mapNotNull { candidate ->
                    LanguageTag.parse(candidate.languageTag)?.let { language ->
                        TextLanguageCandidate(language, candidate.confidence)
                    }
                }
                .distinctBy(TextLanguageCandidate::language)
            val first = candidates.firstOrNull() ?: return TextLanguageDetection.Undetermined
            TextLanguageDetection.Detected(
                language = first.language,
                confidence = first.confidence,
                alternatives = candidates.drop(1),
            )
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
}

fun androidTextClassifierLanguageDetector(
    application: Application,
): TextLanguageDetector? {
    val textClassifier = application
        .getSystemService(TextClassificationManager::class.java)
        ?.textClassifier
        ?: return null
    return AndroidTextClassifierLanguageDetector(
        classify = { text ->
            val result = textClassifier.detectLanguage(
                TextLanguage.Request.Builder(text).build(),
            )
            List(result.localeHypothesisCount) { index ->
                val locale = result.getLocale(index)
                AndroidTextClassifierLanguageDetector.LanguageCandidate(
                    languageTag = locale.toLanguageTag(),
                    confidence = result.getConfidenceScore(locale),
                )
            }
        },
    )
}
