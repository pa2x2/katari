package mihon.translation.runtime

import android.app.Application
import mihon.feature.runtime.ApplicationFeatureRuntimeComponent
import mihon.translation.spi.TranslationEngineContribution
import mihon.translation.spi.TranslationSourceLanguageDetector

/**
 * Variant-specific participation in the Translation runtime.
 *
 * The application Feature module is the only consumer. Reader implementations continue to depend only on
 * [mihon.translation.api.TranslationFeature].
 */
interface TranslationRuntimeComponent : ApplicationFeatureRuntimeComponent {
    fun contribute(application: Application): TranslationRuntimeContribution
}

data class TranslationRuntimeContribution(
    val sourceLanguageDetectors: List<TranslationSourceLanguageDetector> = emptyList(),
    val engineContributions: List<TranslationEngineContribution> = emptyList(),
)
