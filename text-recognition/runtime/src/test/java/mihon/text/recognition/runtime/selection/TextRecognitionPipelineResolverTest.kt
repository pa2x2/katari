package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.runtime.EXAMPLE_PROVIDER
import mihon.text.recognition.runtime.EXCLUDED_PROVIDER
import mihon.text.recognition.runtime.FakeDetector
import mihon.text.recognition.runtime.FakePageImage
import mihon.text.recognition.runtime.FakeRecognizer
import mihon.text.recognition.runtime.JAPANESE
import mihon.text.recognition.runtime.contribution
import mihon.text.recognition.runtime.knownComponent
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
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
