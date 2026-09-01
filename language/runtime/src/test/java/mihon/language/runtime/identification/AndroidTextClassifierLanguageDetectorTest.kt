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
                listOf(AndroidTextClassifierLanguageDetector.LanguageCandidate("fr-FR", 0.8f))
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
            classify = { emptyList() },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )
        val indeterminate = AndroidTextClassifierLanguageDetector(
            classify = {
                listOf(AndroidTextClassifierLanguageDetector.LanguageCandidate("und", 0f))
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        missing.detect("...") shouldBe TextLanguageDetection.Undetermined
        indeterminate.detect("...") shouldBe TextLanguageDetection.Undetermined
    }

    @Test
    fun `ranked platform hypotheses cross the detector boundary without product filtering`() = runTest {
        val detector = AndroidTextClassifierLanguageDetector(
            classify = {
                listOf(
                    AndroidTextClassifierLanguageDetector.LanguageCandidate("fr", 0.49f),
                    AndroidTextClassifierLanguageDetector.LanguageCandidate("en", 0.4f),
                )
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        detector.detect("Ambiguous text") shouldBe TextLanguageDetection.Detected(
            language = LanguageTag.require("fr"),
            confidence = 0.49f,
            alternatives = listOf(
                mihon.language.api.identification.TextLanguageCandidate(
                    language = LanguageTag.require("en"),
                    confidence = 0.4f,
                ),
            ),
        )
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
