package app.katari.text.recognition.runtime.selection

import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.configuration.TextRecognitionConfiguration
import app.katari.text.recognition.api.configuration.TextRecognitionPipelineOrigin
import app.katari.text.recognition.api.configuration.TextRecognitionPipelineResolution
import app.katari.text.recognition.api.pipeline.TextRecognitionPipeline
import app.katari.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import app.katari.text.recognition.runtime.language.readsLanguage
import app.katari.text.recognition.runtime.language.recognitionLanguage
import app.katari.text.recognition.runtime.registry.TextRecognitionComponentRegistry

/**
 * Decides how a configuration reads a language: its override, else its engine's preset, else another engine's
 * preset, so a language keeps working when the chosen engine cannot read it.
 */
internal class TextRecognitionPipelineResolver(
    private val registry: TextRecognitionComponentRegistry,
) {
    fun resolve(configuration: TextRecognitionConfiguration, language: LanguageTag): TextRecognitionPipelineResolution {
        configuration.overrides.entries
            .firstOrNull { (overridden, _) -> overridden.recognitionLanguage == language.recognitionLanguage }
            ?.let { (_, selection) -> return resolveOverride(selection, language) }

        val provider = configuration.provider
            ?.takeIf { id -> registry.includedProviders.any { it.id == id } }
            ?: registry.includedProviders.firstOrNull()?.id
        val usable = registry.presets(language).filter { registry.isIncluded(it.pipeline) }
        usable.firstOrNull { it.provider == provider }?.let { preset ->
            return TextRecognitionPipelineResolution.Resolved(
                preset.pipeline,
                TextRecognitionPipelineOrigin.Engine,
                preset,
            )
        }
        usable.firstOrNull()?.let { preset ->
            return TextRecognitionPipelineResolution.Resolved(
                preset.pipeline,
                TextRecognitionPipelineOrigin.OtherEngine,
                preset,
            )
        }
        return if (registry.supportedLanguages.toSet().readsLanguage(language)) {
            TextRecognitionPipelineResolution.ChoiceRequired
        } else {
            TextRecognitionPipelineResolution.UnsupportedLanguage
        }
    }

    fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? = when (selection) {
        is TextRecognitionPipelineSelection.Preset -> registry.preset(selection.preset)?.pipeline
        is TextRecognitionPipelineSelection.Custom -> selection.pipeline
    }

    private fun resolveOverride(
        selection: TextRecognitionPipelineSelection,
        language: LanguageTag,
    ): TextRecognitionPipelineResolution {
        val pipeline = pipeline(selection)
            ?.takeIf { registry.isWellFormed(it) && registry.reads(it, language) && registry.isIncluded(it) }
            ?: return TextRecognitionPipelineResolution.OverrideUnavailable(selection)
        return TextRecognitionPipelineResolution.Resolved(
            pipeline = pipeline,
            origin = TextRecognitionPipelineOrigin.Override,
            preset = (selection as? TextRecognitionPipelineSelection.Preset)?.let { registry.preset(it.preset) },
        )
    }
}
