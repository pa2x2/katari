package mihon.text.recognition.runtime.registry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
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
import mihon.text.recognition.runtime.preset
import org.junit.jupiter.api.Test

class TextRecognitionComponentRegistryTest {

    private val page = FakePageImage(ImageSize(100, 100))
    private val detector = FakeDetector(page, emptyList())
    private val recognizer = FakeRecognizer(page, emptyMap())
    private val excludedEngine = knownComponent(
        id = "excluded.engine",
        role = TextRecognitionComponentRole.Engine,
        languages = setOf(ENGLISH),
        provider = EXCLUDED_PROVIDER,
    )

    @Test
    fun `engines excluded from the build stay in the catalog but do not add supported languages`() {
        val registry = TextRecognitionComponentRegistry(
            listOf(
                contribution(EXAMPLE_PROVIDER, implementations = listOf(detector, recognizer)),
                contribution(EXCLUDED_PROVIDER, catalogOnly = listOf(excludedEngine)),
            ),
        )

        registry.providers shouldContainExactly listOf(EXAMPLE_PROVIDER, EXCLUDED_PROVIDER)
        registry.includedProviders shouldContainExactly listOf(EXAMPLE_PROVIDER)
        registry.knownComponents.map { it.id } shouldContainExactly
            listOf(detector.catalogEntry.id, recognizer.catalogEntry.id, excludedEngine.id)
        registry.supportedLanguages shouldContainExactly listOf(JAPANESE)
    }

    @Test
    fun `a preset must place components in the roles they implement`() {
        val swapped = preset(
            id = "swapped",
            pipeline = TextRecognitionPipeline.Staged(recognizer.catalogEntry.id, detector.catalogEntry.id),
        )

        shouldThrow<IllegalArgumentException> {
            TextRecognitionComponentRegistry(
                listOf(contribution(EXAMPLE_PROVIDER, listOf(detector, recognizer), presets = listOf(swapped))),
            )
        }.message shouldContain "wrong roles"
    }

    @Test
    fun `a preset cannot claim a language its reader does not support`() {
        val english = preset(
            id = "english",
            pipeline = TextRecognitionPipeline.Staged(detector.catalogEntry.id, recognizer.catalogEntry.id),
            languages = setOf(ENGLISH),
        )

        shouldThrow<IllegalArgumentException> {
            TextRecognitionComponentRegistry(
                listOf(contribution(EXAMPLE_PROVIDER, listOf(detector, recognizer), presets = listOf(english))),
            )
        }.message shouldContain "cannot read en"
    }

    @Test
    fun `candidate pipelines for a language include excluded engines but only recognizers that read it`() {
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
                contribution(EXCLUDED_PROVIDER, catalogOnly = listOf(excludedEngine.copy(languages = setOf(JAPANESE)))),
            ),
        )

        registry.pipelines(JAPANESE) shouldContainExactly listOf(
            TextRecognitionPipeline.Staged(detector.catalogEntry.id, recognizer.catalogEntry.id),
            TextRecognitionPipeline.Engine(excludedEngine.id),
        )
    }
}
