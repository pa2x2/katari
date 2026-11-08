package app.katari.text.recognition.api

import app.katari.text.recognition.api.preparation.ReadyTextRecognition
import app.katari.text.recognition.api.preparation.TextRecognitionPreparation
import app.katari.text.recognition.api.preparation.TextRecognitionSetupPreparation
import app.katari.text.recognition.api.request.TextRecognitionRequest
import app.katari.text.recognition.api.request.TextRecognitionSetupRequest
import app.katari.text.recognition.api.result.TextRecognitionExecution

/**
 * Domain-agnostic recognition of text in images.
 *
 * [prepare] resolves the profile's pipeline for the request language and reports every prerequisite (language,
 * pipeline choice, model downloads) before [recognize] may run. The same prerequisites can be checked for a
 * [TextRecognitionSetupRequest] before any image exists.
 */
interface TextRecognitionFeature {
    suspend fun prepare(request: TextRecognitionRequest): TextRecognitionPreparation

    suspend fun prepare(setup: TextRecognitionSetupRequest): TextRecognitionSetupPreparation

    suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution
}
