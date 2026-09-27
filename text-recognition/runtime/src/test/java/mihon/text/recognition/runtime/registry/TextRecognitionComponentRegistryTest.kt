package mihon.text.recognition.runtime.registry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import mihon.text.recognition.api.component.TextRecognitionBuildAvailability
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.runtime.ENGLISH
import mihon.text.recognition.runtime.FakeDetector
import mihon.text.recognition.runtime.FakePageImage
import mihon.text.recognition.runtime.FakeRecognizer
import mihon.text.recognition.runtime.JAPANESE
import mihon.text.recognition.runtime.knownComponent
import mihon.text.recognition.spi.contribution.TextRecognitionComponentContribution
import mihon.text.recognition.spi.contribution.TextRecognitionPresetContribution
import org.junit.jupiter.api.Test

class TextRecognitionComponentRegistryTest {

    private val page = FakePageImage(ImageSize(100, 100))
    private val detector = FakeDetector(page, emptyList())
    private val recognizer = FakeRecognizer(page, emptyMap())

    @Test
    fun `components excluded from the build stay in the catalog but do not add supported languages`() {
        val excluded = knownComponent(
            id = "excluded.engine",
            role = TextRecognitionComponentRole.Engine,
            languages = setOf(ENGLISH),
            buildAvailability = TextRecognitionBuildAvailability.NotIncluded("Not in this build"),
        )

        val registry = TextRecognitionComponentRegistry(
            contributions = listOf(
                TextRecognitionComponentContribution(detector),
                TextRecognitionComponentContribution(recognizer),
                TextRecognitionComponentContribution(catalogEntry = excluded),
            ),
            presetContributions = emptyList(),
        )

        registry.knownComponents.map { it.id } shouldContainExactly
            listOf(detector.catalogEntry.id, recognizer.catalogEntry.id, excluded.id)
        registry.supportedLanguages shouldContainExactly listOf(JAPANESE)
    }

    @Test
    fun `a preset must place components in the roles they implement`() {
        val swapped = TextRecognitionPreset(
            id = TextRecognitionPresetId("swapped"),
            displayName = "Swapped",
            description = "Detector and recognizer swapped",
            languages = setOf(JAPANESE),
            pipeline = TextRecognitionPipeline.Staged(
                detector = recognizer.catalogEntry.id,
                recognizer = detector.catalogEntry.id,
            ),
        )

        shouldThrow<IllegalArgumentException> {
            TextRecognitionComponentRegistry(
                contributions = listOf(
                    TextRecognitionComponentContribution(detector),
                    TextRecognitionComponentContribution(recognizer),
                ),
                presetContributions = listOf(TextRecognitionPresetContribution(swapped)),
            )
        }.message shouldContain "wrong roles"
    }

    @Test
    fun `a preset cannot claim a language its reader does not support`() {
        val english = TextRecognitionPreset(
            id = TextRecognitionPresetId("english"),
            displayName = "English",
            description = "Claims English",
            languages = setOf(ENGLISH),
            pipeline = TextRecognitionPipeline.Staged(detector.catalogEntry.id, recognizer.catalogEntry.id),
        )

        shouldThrow<IllegalArgumentException> {
            TextRecognitionComponentRegistry(
                contributions = listOf(
                    TextRecognitionComponentContribution(detector),
                    TextRecognitionComponentContribution(recognizer),
                ),
                presetContributions = listOf(TextRecognitionPresetContribution(english)),
            )
        }.message shouldContain "cannot read en"
    }
}
