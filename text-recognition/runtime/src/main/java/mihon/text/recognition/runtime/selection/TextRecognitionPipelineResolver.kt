package mihon.text.recognition.runtime.selection

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.runtime.language.readsLanguage
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry

/** Resolves which pipeline reads a language for the current profile. */
internal class TextRecognitionPipelineResolver(
    private val registry: TextRecognitionComponentRegistry,
    private val preferences: ProfileTextRecognitionPreferences,
) {
    fun resolve(language: LanguageTag): TextRecognitionPipelineResolution {
        val explicit = preferences.selection(language).get()
        if (explicit != null) {
            val pipeline = pipeline(explicit)
                ?.takeIf { registry.isWellFormed(it) && registry.reads(it, language) }
                ?: return TextRecognitionPipelineResolution.InvalidSelection
            return TextRecognitionPipelineResolution.Resolved(pipeline)
        }
        return defaultSelection(language)
            ?.let(::pipeline)
            ?.let(TextRecognitionPipelineResolution::Resolved)
            ?: if (registry.supportedLanguages.toSet().readsLanguage(language)) {
                TextRecognitionPipelineResolution.NothingSelected
            } else {
                TextRecognitionPipelineResolution.UnsupportedLanguage
            }
    }

    /** The first preset for [language] whose components are all part of this build. */
    fun defaultSelection(language: LanguageTag): TextRecognitionPipelineSelection? =
        registry.presets(language)
            .firstOrNull { registry.isIncluded(it.pipeline) }
            ?.let { TextRecognitionPipelineSelection.Preset(it.id) }

    fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? = when (selection) {
        is TextRecognitionPipelineSelection.Preset -> registry.preset(selection.preset)?.pipeline
        is TextRecognitionPipelineSelection.Custom -> selection.pipeline
    }
}

internal sealed interface TextRecognitionPipelineResolution {
    data class Resolved(val pipeline: TextRecognitionPipeline) : TextRecognitionPipelineResolution

    /** The profile's explicit choice no longer names a usable pipeline for the language. */
    data object InvalidSelection : TextRecognitionPipelineResolution

    /** Components read the language, but no preset of this build is available for it. */
    data object NothingSelected : TextRecognitionPipelineResolution

    data object UnsupportedLanguage : TextRecognitionPipelineResolution
}
