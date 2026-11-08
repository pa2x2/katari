package app.katari.text.recognition.spi.contribution

import app.katari.text.recognition.api.component.KnownTextRecognitionComponent
import app.katari.text.recognition.api.pipeline.TextRecognitionPreset
import app.katari.text.recognition.api.provider.KnownTextRecognitionProvider
import app.katari.text.recognition.api.provider.TextRecognitionBuildAvailability
import app.katari.text.recognition.spi.component.TextRecognitionComponent

/**
 * Everything one provider contributes: its catalog entry, its components, and its presets.
 *
 * A provider excluded from the build contributes its catalog without [implementations], so hosts can list it and
 * explain why it cannot be chosen.
 */
data class TextRecognitionProviderContribution(
    val provider: KnownTextRecognitionProvider,
    val components: List<KnownTextRecognitionComponent>,
    val implementations: List<TextRecognitionComponent> = emptyList(),
    val presets: List<TextRecognitionPreset> = emptyList(),
    val order: Int = 0,
) {
    init {
        require(components.all { it.provider == provider.id }) {
            "Text recognition provider ${provider.id.value} lists components of another provider"
        }
        require(presets.all { it.provider == provider.id }) {
            "Text recognition provider ${provider.id.value} lists presets of another provider"
        }
        val included = provider.buildAvailability == TextRecognitionBuildAvailability.Included
        require(implementations.map { it.catalogEntry }.toSet() == if (included) components.toSet() else emptySet()) {
            "Text recognition provider ${provider.id.value} must implement exactly its components when included"
        }
    }
}
