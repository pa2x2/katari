package mihon.text.recognition.ui.models

import io.kotest.matchers.shouldBe
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.DETECTOR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.DETECTOR_MODEL
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.ENGINE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.GENERAL_OCR
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.JAPANESE
import mihon.text.recognition.ui.fixture.MangaCatalogHostActions.Companion.MANGA_OCR_MODEL
import org.junit.jupiter.api.Test

class TextRecognitionStoredModelsTest {

    @Test
    fun `stored models are split into used, usable, and those no component asks for`() {
        val general = TextRecognitionPipelineSelection.Custom(TextRecognitionPipeline(DETECTOR.id, GENERAL_OCR.id))
        val usage = MangaCatalogHostActions().modelUsage(
            TextRecognitionConfiguration(ENGINE.id, mapOf(JAPANESE to general)),
        )
        val detector = StoredModelArtifact(DETECTOR_MODEL, storedBytes = 10, complete = true)
        val mangaOcr = StoredModelArtifact(MANGA_OCR_MODEL, storedBytes = 20, complete = true)
        val removed = StoredModelArtifact(
            MANGA_OCR_MODEL.copy(id = ModelArtifactId("tesseract.english")),
            storedBytes = 4,
            complete = true,
        )

        val sorted = usage.sort(listOf(removed, mangaOcr, detector))

        sorted.inUse.map { it.artifact } shouldBe listOf(detector)
        sorted.inUse.single().everyLanguage shouldBe true
        sorted.unused shouldBe listOf(mangaOcr)
        sorted.obsolete shouldBe listOf(removed)
        sorted.obsoleteBytes shouldBe 4
        sorted.totalBytes shouldBe 34
    }
}
