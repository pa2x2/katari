package mihon.language.runtime.identification

import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AndroidTextClassifierLanguageDetectorTest {

    @Test
    fun `cancellation is never converted into platform unavailability`() = runTest {
        val detector = AndroidTextClassifierLanguageDetector(
            classify = { throw CancellationException() },
            workerDispatcher = StandardTestDispatcher(testScheduler),
        )

        shouldThrow<CancellationException> { detector.detect("text") }
    }
}
