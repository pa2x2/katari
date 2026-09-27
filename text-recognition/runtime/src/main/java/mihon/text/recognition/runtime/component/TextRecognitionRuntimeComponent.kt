package mihon.text.recognition.runtime.component

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.application.instances
import mihon.text.recognition.spi.contribution.TextRecognitionComponentContribution
import mihon.text.recognition.spi.contribution.TextRecognitionPresetContribution

/**
 * Variant-specific participation in the text recognition runtime. Hosts depend only on
 * [mihon.text.recognition.api.TextRecognitionFeature].
 */
interface TextRecognitionRuntimeComponent : ApplicationFeatureRuntimeComponent {
    fun contribute(application: Application): TextRecognitionRuntimeContribution
}

data class TextRecognitionRuntimeContribution(
    val components: List<TextRecognitionComponentContribution> = emptyList(),
    val presets: List<TextRecognitionPresetContribution> = emptyList(),
)

internal fun createTextRecognitionRuntimeContributions(
    application: Application,
    components: ApplicationFeatureRuntimeComponents,
): List<TextRecognitionRuntimeContribution> =
    components.instances<TextRecognitionRuntimeComponent>().map { it.contribute(application) }
