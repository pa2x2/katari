package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.runtime.Composable
import mihon.text.recognition.api.configuration.TextRecognitionPipelineOrigin
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.ui.settings.componentNames
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A preset's name, or the components of a custom pipeline. */
@Composable
internal fun TextRecognitionHostActions.selectionLabel(selection: TextRecognitionPipelineSelection): String =
    when (selection) {
        is TextRecognitionPipelineSelection.Preset ->
            presets.firstOrNull { it.id == selection.preset }?.displayName ?: selection.preset.value
        is TextRecognitionPipelineSelection.Custom -> componentNames(selection.pipeline)
    }

/** How a language is read, naming the other engine when the chosen one cannot read it. */
@Composable
internal fun TextRecognitionHostActions.resolutionLabel(resolution: TextRecognitionPipelineResolution): String? =
    when (resolution) {
        is TextRecognitionPipelineResolution.Resolved -> {
            val name = resolution.preset?.displayName ?: componentNames(resolution.pipeline)
            val provider = resolution.preset?.provider?.let { id -> providers.firstOrNull { it.id == id } }
            if (resolution.origin == TextRecognitionPipelineOrigin.OtherEngine && provider != null) {
                stringResource(MR.strings.text_recognition_settings_via_other_engine, name, provider.name)
            } else {
                name
            }
        }
        else -> null
    }
