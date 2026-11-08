package mihon.presentation.kit.feedback

import dev.icerock.moko.resources.StringResource
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// runCurrent settles the queues between steps, so each step sees what the user would
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenSnackbarsTest {
    private val own = SnackbarHostState()
    private val messagesHost = SnackbarHostState()
    private val snackbars = ScreenSnackbars(own, messagesHost)

    private fun TestScope.screenMessages(): ScreenMessages =
        ScreenMessages(NoOutsideScreen, messagesHost, backgroundScope, outer = null).also { it.claim(Any()) }

    @Test
    fun `a message waits behind a screen snackbar that closes on its own, and a newer message replaces it meanwhile`() =
        runTest {
            val messages = screenMessages()
            val result = async {
                own.showSnackbar("Update done", actionLabel = "Check skipped", duration = SnackbarDuration.Long)
            }
            runCurrent()

            messages.show("Backup done")
            messages.show("New items found")
            runCurrent()

            // Drawing the message now would cover "Check skipped" for the message's whole life
            snackbars.shown shouldBe own
            own.currentSnackbarData!!.performAction()
            result.await() shouldBe SnackbarResult.ActionPerformed
            runCurrent()
            snackbars.shown shouldBe messagesHost
            messagesHost.currentSnackbarData?.visuals?.message shouldBe "New items found"
        }

    @Test
    fun `a never-ending screen snackbar steps aside for a message and comes back after it with its action`() = runTest {
        val messages = screenMessages()
        // An action with no duration never ends, like an Indefinite one
        val result = async { own.showSnackbar("Couldn't load", actionLabel = "Retry") }
        runCurrent()

        messages.show("Backup done")
        runCurrent()
        snackbars.shown shouldBe messagesHost
        result.isCompleted shouldBe false

        messagesHost.currentSnackbarData!!.dismiss()
        runCurrent()
        snackbars.shown shouldBe own
        own.currentSnackbarData!!.performAction()
        result.await() shouldBe SnackbarResult.ActionPerformed
    }

    private object NoOutsideScreen : ScreenMessagePlatform {
        override fun string(resource: StringResource): String = error("Not used")

        override fun showOutsideScreen(message: String, long: Boolean) {
            error("Shown outside the screen: $message")
        }
    }
}
