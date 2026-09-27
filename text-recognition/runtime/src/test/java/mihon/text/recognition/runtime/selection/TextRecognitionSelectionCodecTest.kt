package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import org.junit.jupiter.api.Test

class TextRecognitionSelectionCodecTest {

    @Test
    fun `every selection form survives persistence`() {
        val selections = listOf(
            TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("japanese-manga")),
            TextRecognitionPipelineSelection.Custom(
                TextRecognitionPipeline.Staged(
                    detector = TextRecognitionComponentId("onnx.detector"),
                    recognizer = TextRecognitionComponentId("onnx.manga-ocr"),
                ),
            ),
            TextRecognitionPipelineSelection.Custom(
                TextRecognitionPipeline.Engine(TextRecognitionComponentId("mlkit.japanese")),
            ),
        )

        selections.forEach { selection ->
            TextRecognitionSelectionCodec.decode(TextRecognitionSelectionCodec.encode(selection)) shouldBe selection
        }
    }

    @Test
    fun `unreadable stored values restore the default`() {
        TextRecognitionSelectionCodec.decode("staged:Not An Id:x") shouldBe null
        TextRecognitionSelectionCodec.decode("unknown:value") shouldBe null
    }
}
