package mihon.text.recognition.ui.picker.pipeline

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.DETECTOR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.ENGINE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.GENERAL_OCR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.JAPANESE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.JAPANESE_MANGA
import org.junit.jupiter.api.Test

class TextRecognitionPipelineChoicesTest {

    private val host = MangaCatalogHostActions()
    private val generalPipeline = TextRecognitionPipeline(DETECTOR.id, GENERAL_OCR.id)

    @Test
    fun `automatic names what the engine picks even while the language has a choice of its own`() {
        val chosen = TextRecognitionPipelineSelection.Custom(generalPipeline)
        val configuration = TextRecognitionConfiguration(ENGINE.id, mapOf(LanguageTag.require("ja-JP") to chosen))

        val choices = host.pipelineChoices(configuration, JAPANESE, emptyMap())

        choices.automatic?.option?.selection shouldBe TextRecognitionPipelineSelection.Preset(JAPANESE_MANGA.id)
        choices.automatic?.recommendedBy shouldBe ENGINE
        choices.current shouldBe chosen
        choices.currentUnavailable shouldBe false
    }
}
