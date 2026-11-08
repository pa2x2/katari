package eu.kanade.presentation.more.settings.screen.textrecognition

import app.katari.text.recognition.api.host.TextRecognitionHostActions

/**
 * Graph entry points of the text recognition settings beyond their model, such as whether the settings list offers
 * them.
 */
interface TextRecognitionSettingsGraph {
    val textRecognitionHostActions: TextRecognitionHostActions
}
