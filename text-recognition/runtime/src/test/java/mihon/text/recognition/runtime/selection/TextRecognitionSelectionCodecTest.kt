package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import org.junit.jupiter.api.Test

class TextRecognitionSelectionCodecTest {

    @Test
    fun `stored selections keep their format, and unreadable values restore the default`() {
        val preset = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("onnx.japanese-manga"))
        val custom = TextRecognitionPipelineSelection.Custom(
            TextRecognitionPipeline(
                detector = TextRecognitionComponentId("onnx.comic-text-and-bubble-detector"),
                recognizer = TextRecognitionComponentId("onnx.manga-ocr"),
            ),
        )
        val storedCustom = "staged:onnx.comic-text-and-bubble-detector:onnx.manga-ocr"

        TextRecognitionSelectionCodec.encode(preset) shouldBe "preset:onnx.japanese-manga"
        TextRecognitionSelectionCodec.decode("preset:onnx.japanese-manga") shouldBe preset
        TextRecognitionSelectionCodec.encode(custom) shouldBe storedCustom
        TextRecognitionSelectionCodec.decode(storedCustom) shouldBe custom
        TextRecognitionSelectionCodec.decode("staged:Not An Id:x") shouldBe null
        TextRecognitionSelectionCodec.decode("unknown:value") shouldBe null
    }
}
