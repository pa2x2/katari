package app.katari.text.recognition.runtime.selection

import app.katari.text.recognition.api.component.TextRecognitionComponentRole
import app.katari.text.recognition.api.configuration.TextRecognitionConfiguration
import app.katari.text.recognition.api.configuration.TextRecognitionPipelineResolution
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.pipeline.TextRecognitionPipeline
import app.katari.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import app.katari.text.recognition.runtime.EXAMPLE_PROVIDER
import app.katari.text.recognition.runtime.EXCLUDED_PROVIDER
import app.katari.text.recognition.runtime.FakeDetector
import app.katari.text.recognition.runtime.FakePageImage
import app.katari.text.recognition.runtime.FakeRecognizer
import app.katari.text.recognition.runtime.JAPANESE
import app.katari.text.recognition.runtime.contribution
import app.katari.text.recognition.runtime.knownComponent
import app.katari.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TextRecognitionPipelineResolverTest {

    @Test
    fun `an override that names an excluded component is reported instead of silently replaced`() {
        val page = FakePageImage(ImageSize(100, 100))
        val detector = FakeDetector(page, emptyList())
        val recognizer = FakeRecognizer(page, emptyMap())
        val excludedRecognizer = knownComponent(
            id = "excluded.recognizer",
            role = TextRecognitionComponentRole.Recognizer,
            provider = EXCLUDED_PROVIDER,
        )
        val resolver = TextRecognitionPipelineResolver(
            TextRecognitionComponentRegistry(
                listOf(
                    contribution(EXAMPLE_PROVIDER, listOf(detector, recognizer), order = 0),
                    contribution(EXCLUDED_PROVIDER, catalogOnly = listOf(excludedRecognizer), order = 1),
                ),
            ),
        )
        val selection = TextRecognitionPipelineSelection.Custom(
            TextRecognitionPipeline(detector.catalogEntry.id, excludedRecognizer.id),
        )
        val configuration = TextRecognitionConfiguration(provider = null, overrides = mapOf(JAPANESE to selection))

        resolver.resolve(configuration, JAPANESE) shouldBe
            TextRecognitionPipelineResolution.OverrideUnavailable(selection)
    }
}
