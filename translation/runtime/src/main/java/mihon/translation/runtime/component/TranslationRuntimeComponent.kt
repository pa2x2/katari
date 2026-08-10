package mihon.translation.runtime.component

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.translation.spi.contribution.TranslationEngineContribution

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
    val engineContributions: List<TranslationEngineContribution> = emptyList(),
)
