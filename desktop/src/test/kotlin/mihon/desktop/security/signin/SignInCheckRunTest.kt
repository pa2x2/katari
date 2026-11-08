package mihon.desktop.security.signin

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class SignInCheckRunTest {

    // PAM counts an unanswered question that ends in an error as a failed try, which can lock the account; only a
    // check stopped while it waits counts nothing. The check's output is read in a blocking call, so a collection that
    // waits for that read to end would never stop the process.
    @Test
    fun `closing the dialog while the system waits for the password stops the check`() {
        // Stands in for a check waiting on its question: it says nothing and doesn't end
        val process = ProcessBuilder("sleep", "60").start()
        val run = SignInCheckRun(process)
        runBlocking {
            val dialog = launch(Dispatchers.Default) { run.events.collect {} }
            delay(200)
            withTimeout(5_000) { dialog.cancelAndJoin() }
        }

        process.waitFor(5, TimeUnit.SECONDS) shouldBe true
    }
}
