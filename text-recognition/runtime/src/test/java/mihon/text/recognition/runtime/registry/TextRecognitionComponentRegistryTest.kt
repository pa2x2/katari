package mihon.text.recognition.runtime.registry

import io.kotest.matchers.collections.shouldContainExactly
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.runtime.ENGLISH
import mihon.text.recognition.runtime.EXAMPLE_PROVIDER
import mihon.text.recognition.runtime.EXCLUDED_PROVIDER
import mihon.text.recognition.runtime.FakeDetector
import mihon.text.recognition.runtime.FakePageImage
import mihon.text.recognition.runtime.FakeRecognizer
import mihon.text.recognition.runtime.JAPANESE
import mihon.text.recognition.runtime.contribution
import mihon.text.recognition.runtime.knownComponent
import org.junit.jupiter.api.Test

class TextRecognitionComponentRegistryTest {

    private val page = FakePageImage(ImageSize(100, 100))
    private val detector = FakeDetector(page, emptyList())
    private val recognizer = FakeRecognizer(page, emptyMap())
    private val excludedRecognizer = knownComponent(
        id = "excluded.recognizer",
        role = TextRecognitionComponentRole.Recognizer,
        languages = setOf(ENGLISH),
        provider = EXCLUDED_PROVIDER,
    )

    @Test
    fun `components excluded from the build stay in the catalog but do not add supported languages`() {
        val registry = TextRecognitionComponentRegistry(
            listOf(
                contribution(EXAMPLE_PROVIDER, implementations = listOf(detector, recognizer)),
                contribution(EXCLUDED_PROVIDER, catalogOnly = listOf(excludedRecognizer)),
            ),
        )

        registry.knownComponents.map { it.id } shouldContainExactly
            listOf(detector.catalogEntry.id, recognizer.catalogEntry.id, excludedRecognizer.id)
        registry.supportedLanguages shouldContainExactly listOf(JAPANESE)
    }

    @Test
    fun `candidate pipelines for a language include excluded components but only recognizers that read it`() {
        val englishRecognizer = FakeRecognizer(
            page = page,
            texts = emptyMap(),
            catalogEntry = knownComponent(
                "english.recognizer",
                TextRecognitionComponentRole.Recognizer,
                setOf(ENGLISH),
            ),
        )
        val registry = TextRecognitionComponentRegistry(
            listOf(
                contribution(EXAMPLE_PROVIDER, listOf(detector, recognizer, englishRecognizer)),
                contribution(
                    EXCLUDED_PROVIDER,
                    catalogOnly = listOf(excludedRecognizer.copy(languages = setOf(JAPANESE))),
                ),
            ),
        )

        registry.pipelines(JAPANESE) shouldContainExactly listOf(
            TextRecognitionPipeline(detector.catalogEntry.id, recognizer.catalogEntry.id),
            TextRecognitionPipeline(detector.catalogEntry.id, excludedRecognizer.id),
        )
    }
}
