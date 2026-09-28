package mihon.text.recognition.ui.models

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.DETECTOR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.DETECTOR_MODEL
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.ENGINE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.ENGLISH
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.GENERAL_OCR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.GENERAL_OCR_MODEL
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.JAPANESE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.MANGA_OCR_MODEL
import org.junit.jupiter.api.Test

class TextRecognitionModelUsageTest {

    private val host = MangaCatalogHostActions()

    @Test
    fun `each model lists the languages the configuration reads with it`() {
        val usage = host.modelUsage(TextRecognitionConfiguration(ENGINE.id))

        usage.languages shouldContainExactly mapOf(
            DETECTOR_MODEL.id to listOf(JAPANESE, ENGLISH),
            MANGA_OCR_MODEL.id to listOf(JAPANESE),
            GENERAL_OCR_MODEL.id to listOf(ENGLISH),
        )
        usage.readableLanguages shouldBe 2
    }

    @Test
    fun `a model the configuration stopped using stays known to the catalog`() {
        val general = TextRecognitionPipelineSelection.Custom(TextRecognitionPipeline(DETECTOR.id, GENERAL_OCR.id))
        val usage = host.modelUsage(TextRecognitionConfiguration(ENGINE.id, mapOf(JAPANESE to general)))

        usage.languages.keys shouldBe setOf(DETECTOR_MODEL.id, GENERAL_OCR_MODEL.id)
        usage.known shouldBe setOf(DETECTOR_MODEL.id, MANGA_OCR_MODEL.id, GENERAL_OCR_MODEL.id)
    }
}
