package mihon.desktop.security.signin

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The sign-in checks the app waits on, such as a locked profile's unlock, shown one at a time as a dialog in the app's
 * window ([SignInPromptHost]); a second one waits for the first to end.
 */
@SingleIn(AppScope::class)
@Inject
class SignInPrompts {
    private val turns = Mutex()
    private val showing = MutableStateFlow<SignInRequest?>(null)

    internal val current: StateFlow<SignInRequest?> = showing.asStateFlow()

    /**
     * Asks the user to sign in, under [title] and an optional [detail] that says what for, with [confirmLabel] on the
     * button that sends the password. True once the system accepts the sign-in, false when the user cancels.
     */
    suspend fun ask(title: String, detail: String?, confirmLabel: String): Boolean = turns.withLock {
        val request = SignInRequest(title, detail, confirmLabel)
        showing.value = request
        try {
            request.answer.await()
        } finally {
            showing.value = null
        }
    }
}

internal class SignInRequest(val title: String, val detail: String?, val confirmLabel: String) {
    val answer = CompletableDeferred<Boolean>()
}
