package mihon.text.recognition.ui.models

import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.host.TextRecognitionHostActions

/** What stored models are for: the languages a configuration reads with each, and which models the catalog knows. */
data class TextRecognitionModelUsage(
    /** Languages the configuration reads with each model, in catalog order. */
    val languages: Map<ModelArtifactId, List<LanguageTag>>,
    /** Every model some pipeline of this build can need, whether or not the configuration uses it. */
    val known: Set<ModelArtifactId>,
    /** How many languages the configuration can read at all. */
    val readableLanguages: Int,
)

/** How [configuration] uses the models this build's text recognition catalog can ask for. */
fun TextRecognitionHostActions.modelUsage(configuration: TextRecognitionConfiguration): TextRecognitionModelUsage {
    val languages = linkedMapOf<ModelArtifactId, MutableList<LanguageTag>>()
    var readable = 0
    supportedLanguages.forEach { language ->
        val resolved = resolve(configuration, language) as? TextRecognitionPipelineResolution.Resolved
            ?: return@forEach
        readable += 1
        models(resolved.pipeline, language).forEach { model ->
            languages.getOrPut(model.id, ::mutableListOf) += language
        }
    }
    val known = supportedLanguages
        .flatMap { language ->
            (presets(language).map { it.pipeline } + pipelines(language)).flatMap { models(it, language) }
        }
        .mapTo(mutableSetOf()) { it.id }
    return TextRecognitionModelUsage(languages = languages, known = known, readableLanguages = readable)
}
