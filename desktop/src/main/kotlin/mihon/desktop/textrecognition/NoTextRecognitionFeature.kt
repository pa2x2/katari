package mihon.desktop.textrecognition

import dev.zacsweers.metro.Inject
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.preparation.ReadyTextRecognition
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.preparation.TextRecognitionRequirement
import mihon.text.recognition.api.preparation.TextRecognitionSetupPreparation
import mihon.text.recognition.api.preparation.TextRecognitionUnavailableReason
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.request.TextRecognitionSetupRequest
import mihon.text.recognition.api.result.TextRecognitionExecution

/**
 * Recognition with no component to read text: no language is read, as on an Android build without any engine. Stands
 * in until desktop has a recognition engine.
 */
@Inject
class NoTextRecognitionFeature : TextRecognitionFeature {
    override suspend fun prepare(request: TextRecognitionRequest): TextRecognitionPreparation =
        unsupported(request.language)

    override suspend fun prepare(setup: TextRecognitionSetupRequest): TextRecognitionSetupPreparation =
        unsupported(setup.language)

    override suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution =
        TextRecognitionExecution.Failed()

    private fun unsupported(language: LanguageTag?): TextRecognitionRequirement = if (language == null) {
        TextRecognitionPreparation.LanguageRequired(supportedLanguages = emptyList())
    } else {
        TextRecognitionPreparation.Unavailable(TextRecognitionUnavailableReason.UnsupportedLanguage(language))
    }
}
