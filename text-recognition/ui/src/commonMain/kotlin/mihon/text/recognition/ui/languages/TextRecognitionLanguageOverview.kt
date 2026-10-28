package mihon.text.recognition.ui.languages

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineOrigin
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.ui.settings.primary

/** How a configuration reads every language the build can read, in the groups the Languages screen lists. */
data class TextRecognitionLanguageOverview(
    /** Languages the profile chose a pipeline for. */
    val choices: List<TextRecognitionLanguageChoice>,
    /** Languages read with a preset of the configured engine. */
    val engineGroups: List<TextRecognitionLanguageGroup>,
    /** Languages the configured engine cannot read, read with a preset of another engine. */
    val otherEngineGroups: List<TextRecognitionLanguageGroup>,
    /** Languages some component reads but no preset covers, so the profile has to choose a pipeline. */
    val choiceRequired: List<LanguageTag>,
)

/** A language the profile chose [selection] for; [pipeline] is `null` when this build cannot run the choice. */
data class TextRecognitionLanguageChoice(
    val language: LanguageTag,
    val selection: TextRecognitionPipelineSelection,
    val pipeline: TextRecognitionPipeline?,
)

/** Languages one [preset] reads, in catalog order. */
data class TextRecognitionLanguageGroup(
    val preset: TextRecognitionPreset,
    val languages: List<LanguageTag>,
)

/**
 * Groups [languages] by how [resolve] reads them under [configuration]. Overridden languages, matched by primary
 * language like the resolver does, are listed only as choices.
 */
fun textRecognitionLanguageOverview(
    languages: List<LanguageTag>,
    configuration: TextRecognitionConfiguration,
    resolve: (LanguageTag) -> TextRecognitionPipelineResolution,
): TextRecognitionLanguageOverview {
    val choices = configuration.overrides.map { (language, selection) ->
        TextRecognitionLanguageChoice(
            language = language,
            selection = selection,
            pipeline = (resolve(language) as? TextRecognitionPipelineResolution.Resolved)?.pipeline,
        )
    }
    val overridden = configuration.overrides.keys.mapTo(mutableSetOf()) { it.primary }
    val engineGroups = linkedMapOf<TextRecognitionPreset, MutableList<LanguageTag>>()
    val otherEngineGroups = linkedMapOf<TextRecognitionPreset, MutableList<LanguageTag>>()
    val choiceRequired = mutableListOf<LanguageTag>()
    languages.filterNot { it.primary in overridden }.forEach { language ->
        when (val resolution = resolve(language)) {
            is TextRecognitionPipelineResolution.Resolved -> {
                val preset = resolution.preset ?: return@forEach
                val groups = when (resolution.origin) {
                    TextRecognitionPipelineOrigin.OtherEngine -> otherEngineGroups
                    TextRecognitionPipelineOrigin.Engine,
                    TextRecognitionPipelineOrigin.Override,
                    -> engineGroups
                }
                groups.getOrPut(preset, ::mutableListOf) += language
            }
            TextRecognitionPipelineResolution.ChoiceRequired -> choiceRequired += language
            is TextRecognitionPipelineResolution.OverrideUnavailable,
            TextRecognitionPipelineResolution.UnsupportedLanguage,
            -> Unit
        }
    }
    return TextRecognitionLanguageOverview(
        choices = choices,
        engineGroups = engineGroups.map { (preset, members) -> TextRecognitionLanguageGroup(preset, members) },
        otherEngineGroups = otherEngineGroups.map { (preset, members) ->
            TextRecognitionLanguageGroup(preset, members)
        },
        choiceRequired = choiceRequired,
    )
}
