package mihon.translation.mlkit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.spi.TranslationSourceLanguageDetection
import org.junit.jupiter.api.Test

class MlKitSourceLanguageDetectorTest {

    @Test
    fun `determinate BCP-47 result crosses the provider boundary`() = runTest {
        val detector = MlKitSourceLanguageDetector { "pt-BR" }

        detector.detect("Olá") shouldBe TranslationSourceLanguageDetection.Detected(
            TranslationLanguageTag.require("pt-BR"),
        )
    }

    @Test
    fun `undetermined provider result stays undetermined`() = runTest {
        val detector = MlKitSourceLanguageDetector { "und" }

        detector.detect("...") shouldBe TranslationSourceLanguageDetection.Undetermined
    }

    @Test
    fun `cancellation is never converted into provider unavailability`() = runTest {
        val detector = MlKitSourceLanguageDetector { throw CancellationException() }

        shouldThrow<CancellationException> { detector.detect("text") }
    }
}
