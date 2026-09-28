package mihon.text.recognition.runtime.selection

import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId

/** Persisted form of a pipeline selection. Unreadable values decode to no selection, restoring the default. */
internal object TextRecognitionSelectionCodec {
    fun encode(selection: TextRecognitionPipelineSelection?): String = when (selection) {
        null -> ""
        is TextRecognitionPipelineSelection.Preset -> "$PRESET:${selection.preset.value}"
        is TextRecognitionPipelineSelection.Custom ->
            "$STAGED:${selection.pipeline.detector.value}:${selection.pipeline.recognizer.value}"
    }

    fun decode(value: String): TextRecognitionPipelineSelection? {
        val parts = value.split(':')
        return runCatching {
            when {
                parts.size == 2 && parts[0] == PRESET ->
                    TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId(parts[1]))
                parts.size == 3 && parts[0] == STAGED -> TextRecognitionPipelineSelection.Custom(
                    TextRecognitionPipeline(
                        detector = TextRecognitionComponentId(parts[1]),
                        recognizer = TextRecognitionComponentId(parts[2]),
                    ),
                )
                else -> null
            }
        }.getOrNull()
    }

    private const val PRESET = "preset"
    private const val STAGED = "staged"
}
