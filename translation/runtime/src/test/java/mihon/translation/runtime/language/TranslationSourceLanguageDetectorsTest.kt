package mihon.translation.runtime

import android.app.Application
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import mihon.feature.runtime.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.RegisteredApplicationFeatureRuntimeComponent
import mihon.translation.spi.TranslationSourceLanguageDetection
import mihon.translation.spi.TranslationSourceLanguageDetector
import mihon.translation.spi.TranslationSourceLanguageDetectorId
import org.junit.jupiter.api.Test

class TranslationSourceLanguageDetectorsTest {

    @Test
    fun `typed Translation components contribute detectors`() {
        val detector = FakeDetector("example.detector")
        val components = ApplicationFeatureRuntimeComponents(
            listOf(
                RegisteredApplicationFeatureRuntimeComponent(
                    id = "example.translation-component",
                    component = FakeTranslationRuntimeComponent(listOf(detector)),
                ),
            ),
        )

        createTranslationSourceLanguageDetectors(
            application = mockk(relaxed = true),
            components = components,
        ) shouldContainExactly listOf(detector)
    }

    @Test
    fun `duplicate detector identities fail installation`() {
        val components = ApplicationFeatureRuntimeComponents(
            listOf(
                RegisteredApplicationFeatureRuntimeComponent(
                    id = "example.first-component",
                    component = FakeTranslationRuntimeComponent(listOf(FakeDetector("example.same"))),
                ),
                RegisteredApplicationFeatureRuntimeComponent(
                    id = "example.second-component",
                    component = FakeTranslationRuntimeComponent(listOf(FakeDetector("example.same"))),
                ),
            ),
        )

        val error = shouldThrow<IllegalStateException> {
            createTranslationSourceLanguageDetectors(
                application = mockk(relaxed = true),
                components = components,
            )
        }

        error.message shouldContain "duplicate ids"
    }

    private class FakeTranslationRuntimeComponent(
        private val detectors: List<TranslationSourceLanguageDetector>,
    ) : TranslationRuntimeComponent {
        override fun contribute(application: Application) = TranslationRuntimeContribution(detectors)
    }

    private class FakeDetector(
        id: String,
    ) : TranslationSourceLanguageDetector {
        override val id = TranslationSourceLanguageDetectorId(id)

        override suspend fun detect(text: String) = TranslationSourceLanguageDetection.Undetermined
    }
}
