package mihon.text.recognition.ui.settings

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.DETECTOR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.ENGLISH
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.GENERAL_OCR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.JAPANESE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.JAPANESE_MANGA
import org.junit.jupiter.api.Test

class TextRecognitionPlaygroundRerunTest {

    private val requests = mutableListOf<TextRecognitionRequest>()
    private val feature = mockk<TextRecognitionFeature> {
        coEvery { prepare(capture(requests)) } throws IllegalStateException("not recognized in tests")
    }
    private val generalPipeline = TextRecognitionPipeline(DETECTOR.id, GENERAL_OCR.id)

    @Test
    fun `a draft edit that changes how the tried language is read reads the image again`() {
        val controller = playgroundController()
        controller.runPlayground(mockk(), mockk())

        controller.setDraftOverride(JAPANESE, TextRecognitionPipelineSelection.Custom(generalPipeline))
        controller.discard()

        requests.map { it.pipeline } shouldBe listOf(JAPANESE_MANGA.pipeline, generalPipeline, JAPANESE_MANGA.pipeline)
    }

    @Test
    fun `a draft edit for another language leaves the tried image alone`() {
        val controller = playgroundController()
        controller.runPlayground(mockk(), mockk())

        controller.setDraftOverride(ENGLISH, TextRecognitionPipelineSelection.Custom(generalPipeline))

        requests.map { it.pipeline } shouldBe listOf(JAPANESE_MANGA.pipeline)
    }

    /** Reads run to completion inside the call that starts them, because the fake feature never suspends. */
    private fun playgroundController() = TextRecognitionSettingsController(
        feature = feature,
        hostActions = MangaCatalogHostActions(),
        modelStore = mockk(),
        scope = CoroutineScope(Dispatchers.Unconfined),
        initialPlaygroundLanguage = JAPANESE,
    )
}
