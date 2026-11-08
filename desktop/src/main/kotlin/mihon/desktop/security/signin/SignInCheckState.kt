package mihon.desktop.security.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where one sign-in dialog is: what the system said during the current try, the question it waits on, whether an
 * answer is being checked, and how the last try ended. A refused try starts a new one that keeps [refused] showing
 * until the next answer goes.
 */
internal class SignInCheckState {
    val messages = mutableStateListOf<SignInCheckEvent.Message>()

    var question by mutableStateOf<SignInCheckEvent.Question?>(null)
        private set

    var checking by mutableStateOf(false)
        private set

    var refused by mutableStateOf(false)
        private set

    /** The check couldn't run; a new try starts only when the user asks for it. */
    var failed by mutableStateOf(false)
        private set

    /** The system is busy before it asks anything, such as a fingerprint reader waiting for a finger. */
    val waiting: Boolean
        get() = question == null && messages.isNotEmpty() && !checking && !failed

    fun startTry() {
        messages.clear()
        question = null
        checking = false
        failed = false
    }

    /** Takes [event] in; returns the outcome when the try ended. */
    fun take(event: SignInCheckEvent): SignInOutcome? {
        when (event) {
            is SignInCheckEvent.Message -> messages += event
            is SignInCheckEvent.Question -> {
                question = event
                checking = false
            }
            is SignInCheckEvent.Finished -> {
                question = null
                checking = false
                when (event.outcome) {
                    SignInOutcome.Accepted -> Unit
                    SignInOutcome.Refused -> refused = true
                    SignInOutcome.Failed -> failed = true
                }
                return event.outcome
            }
        }
        return null
    }

    fun answered() {
        question = null
        checking = true
        refused = false
    }

    fun couldNotStart() {
        failed = true
    }
}
