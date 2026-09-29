package mihon.text.recognition.api

import mihon.text.recognition.api.preparation.ReadyTextRecognition
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.result.TextRecognitionExecution

/**
 * Domain-agnostic recognition of text in images.
 *
 * [prepare] resolves the profile's pipeline for the request language and reports every prerequisite (language,
 * pipeline choice, model downloads) before [recognize] may run.
 */
interface TextRecognitionFeature {
    suspend fun prepare(request: TextRecognitionRequest): TextRecognitionPreparation

    suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution
}
