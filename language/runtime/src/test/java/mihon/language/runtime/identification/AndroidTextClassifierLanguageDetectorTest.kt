package mihon.language.runtime.identification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageDetection
import org.junit.jupiter.api.Test

class AndroidTextClassifierLanguageDetectorTest {

    @Test
    fun `indeterminate platform candidate stays undetermined`() = runTest {
        val detector = AndroidTextClassifierLanguageDetector(
            classify = {
                listOf(AndroidTextClassifierLanguageDetector.LanguageCandidate("und", 0f))
            },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        detector.detect("...") shouldBe TextLanguageDetection.Undetermined
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
