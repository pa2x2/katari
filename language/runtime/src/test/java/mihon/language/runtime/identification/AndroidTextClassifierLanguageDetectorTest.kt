package mihon.language.runtime.identification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class AndroidTextClassifierLanguageDetectorTest {

    @Test
    fun `platform locale and confidence cross the detector boundary`() = runTest {
        val detector = AndroidTextClassifierLanguageDetector(
            classify = {
                AndroidTextClassifierLanguageDetector.LanguageCandidate("fr-FR", 0.8f)
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        detector.detect("Bonjour") shouldBe TextLanguageDetection.Detected(
            language = LanguageTag.require("fr-FR"),
            confidence = 0.8f,
        )
    }

    @Test
    fun `missing or indeterminate platform candidate stays undetermined`() = runTest {
        val missing = AndroidTextClassifierLanguageDetector(
            classify = { null },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )
        val indeterminate = AndroidTextClassifierLanguageDetector(
            classify = {
                AndroidTextClassifierLanguageDetector.LanguageCandidate("und", 0f)
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        missing.detect("...") shouldBe TextLanguageDetection.Undetermined
        indeterminate.detect("...") shouldBe TextLanguageDetection.Undetermined
    }

    @Test
    fun `low confidence platform candidate stays undetermined for user correction`() = runTest {
        val detector = AndroidTextClassifierLanguageDetector(
            classify = {
                AndroidTextClassifierLanguageDetector.LanguageCandidate("fr", 0.49f)
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        detector.detect("Ambiguous text") shouldBe TextLanguageDetection.Undetermined
    }

    @Test
    fun `cancellation is never converted into platform unavailability`() = runTest {
        val detector = AndroidTextClassifierLanguageDetector(
            classify = { throw CancellationException() },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        shouldThrow<CancellationException> { detector.detect("text") }
    }
}
