package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import org.junit.jupiter.api.Test

class TextRecognitionSelectionCodecTest {

    @Test
    fun `a preset is stored by its id`() {
        val selection = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("onnx.japanese-manga"))

        TextRecognitionSelectionCodec.encode(selection) shouldBe "preset:onnx.japanese-manga"
        TextRecognitionSelectionCodec.decode("preset:onnx.japanese-manga") shouldBe selection
    }

    @Test
    fun `a custom pipeline is stored as its detector and recognizer`() {
        val selection = TextRecognitionPipelineSelection.Custom(
            TextRecognitionPipeline(
                detector = TextRecognitionComponentId("onnx.comic-text-and-bubble-detector"),
                recognizer = TextRecognitionComponentId("onnx.manga-ocr"),
            ),
        )
        val stored = "staged:onnx.comic-text-and-bubble-detector:onnx.manga-ocr"

        TextRecognitionSelectionCodec.encode(selection) shouldBe stored
        TextRecognitionSelectionCodec.decode(stored) shouldBe selection
    }

    @Test
    fun `unreadable stored values restore the default`() {
        TextRecognitionSelectionCodec.decode("staged:Not An Id:x") shouldBe null
        TextRecognitionSelectionCodec.decode("unknown:value") shouldBe null
    }
}
