package mihon.translation.runtime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.spi.TranslationSourceLanguageDetection
import org.junit.jupiter.api.Test

class AndroidTextClassifierSourceLanguageDetectorTest {

    @Test
    fun `platform locale and confidence cross the detector boundary`() = runTest {
        val detector = AndroidTextClassifierSourceLanguageDetector(
            classify = {
                AndroidTextClassifierSourceLanguageDetector.LanguageCandidate("fr-FR", 0.8f)
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        detector.detect("Bonjour") shouldBe TranslationSourceLanguageDetection.Detected(
            language = TranslationLanguageTag.require("fr-FR"),
            confidence = 0.8f,
        )
    }

    @Test
    fun `missing or indeterminate platform candidate stays undetermined`() = runTest {
        val missing = AndroidTextClassifierSourceLanguageDetector(
            classify = { null },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )
        val indeterminate = AndroidTextClassifierSourceLanguageDetector(
            classify = {
                AndroidTextClassifierSourceLanguageDetector.LanguageCandidate("und", 0f)
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        missing.detect("...") shouldBe TranslationSourceLanguageDetection.Undetermined
        indeterminate.detect("...") shouldBe TranslationSourceLanguageDetection.Undetermined
    }

    @Test
    fun `cancellation is never converted into platform unavailability`() = runTest {
        val detector = AndroidTextClassifierSourceLanguageDetector(
            classify = { throw CancellationException() },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        shouldThrow<CancellationException> { detector.detect("text") }
    }
}
