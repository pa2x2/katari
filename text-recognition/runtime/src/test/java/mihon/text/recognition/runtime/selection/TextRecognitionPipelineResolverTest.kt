package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineOrigin
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
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
import mihon.text.recognition.runtime.provider
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import org.junit.jupiter.api.Test

class TextRecognitionPipelineResolverTest {

    private val page = FakePageImage(ImageSize(100, 100))
    private val secondProvider = provider("second")
    private val detector = FakeDetector(page, emptyList())
    private val japanese = FakeRecognizer(page, emptyMap())
    private val english = FakeRecognizer(
        page = page,
        texts = emptyMap(),
        catalogEntry = knownComponent(
            id = "second.english",
            role = TextRecognitionComponentRole.Recognizer,
            languages = setOf(ENGLISH),
            provider = secondProvider,
        ),
    )
    private val secondDetector = FakeDetector(
        page = page,
        objects = emptyList(),
        catalogEntry = knownComponent(
            "second.detector",
            TextRecognitionComponentRole.Detector,
            provider = secondProvider,
        ),
    )
    private val excludedRecognizer = knownComponent(
        id = "excluded.recognizer",
        role = TextRecognitionComponentRole.Recognizer,
        provider = EXCLUDED_PROVIDER,
    )
    private val japaneseManga = preset(
        id = "example.japanese",
        pipeline = TextRecognitionPipeline(detector.catalogEntry.id, japanese.catalogEntry.id),
    )
    private val englishComics = preset(
        id = "second.english",
        pipeline = TextRecognitionPipeline(secondDetector.catalogEntry.id, english.catalogEntry.id),
        languages = setOf(ENGLISH),
        provider = secondProvider,
    )
    private val resolver = TextRecognitionPipelineResolver(
        TextRecognitionComponentRegistry(
            listOf(
                contribution(EXAMPLE_PROVIDER, listOf(detector, japanese), presets = listOf(japaneseManga), order = 0),
                contribution(
                    secondProvider,
                    listOf(secondDetector, english),
                    presets = listOf(englishComics),
                    order = 1,
                ),
                contribution(EXCLUDED_PROVIDER, catalogOnly = listOf(excludedRecognizer), order = 2),
            ),
        ),
    )

    @Test
    fun `the engine's preset reads its languages and other engines cover the rest`() {
        val configuration = TextRecognitionConfiguration(provider = EXAMPLE_PROVIDER.id)

        resolver.resolve(configuration, LanguageTag.require("ja-JP")) shouldBe
            TextRecognitionPipelineResolution.Resolved(
                japaneseManga.pipeline,
                TextRecognitionPipelineOrigin.Engine,
                japaneseManga,
            )
        resolver.resolve(configuration, ENGLISH) shouldBe TextRecognitionPipelineResolution.Resolved(
            englishComics.pipeline,
            TextRecognitionPipelineOrigin.OtherEngine,
            englishComics,
        )
    }

    @Test
    fun `an override replaces the engine's choice for its language only`() {
        val custom = TextRecognitionPipeline(secondDetector.catalogEntry.id, japanese.catalogEntry.id)
        val configuration = TextRecognitionConfiguration(
            provider = EXAMPLE_PROVIDER.id,
            overrides = mapOf(JAPANESE to TextRecognitionPipelineSelection.Custom(custom)),
        )

        resolver.resolve(configuration, JAPANESE) shouldBe
            TextRecognitionPipelineResolution.Resolved(custom, TextRecognitionPipelineOrigin.Override)
        (resolver.resolve(configuration, ENGLISH) as TextRecognitionPipelineResolution.Resolved).pipeline shouldBe
            englishComics.pipeline
    }

    @Test
    fun `an override that names an excluded component is reported instead of silently replaced`() {
        val selection = TextRecognitionPipelineSelection.Custom(
            TextRecognitionPipeline(detector.catalogEntry.id, excludedRecognizer.id),
        )
        val configuration = TextRecognitionConfiguration(provider = null, overrides = mapOf(JAPANESE to selection))

        resolver.resolve(configuration, JAPANESE) shouldBe
            TextRecognitionPipelineResolution.OverrideUnavailable(selection)
    }

    @Test
    fun `languages no included component reads are unsupported`() {
        resolver.resolve(TextRecognitionConfiguration(provider = null), LanguageTag.require("ko")) shouldBe
            TextRecognitionPipelineResolution.UnsupportedLanguage
    }
}
