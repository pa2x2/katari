package eu.kanade.tachiyomi.ui.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

internal sealed interface BiometricAuthenticationResult {
    data object Success : BiometricAuthenticationResult

    data class Error(val message: CharSequence) : BiometricAuthenticationResult
}

/**
 * Owns the single authentication session a Katari activity may run at a time.
 *
 * This deliberately uses the per-activity [BiometricPrompt] instead of androidx.biometric 1.4's
 * `registerForAuthenticationResult`. That launcher keeps its cancellation signal and pending-result flag in
 * process-wide state, and every activity that has launched it once keeps cancelling the shared session whenever
 * it stops and keeps competing for its result. An app-unlock prompt in [UnlockActivity] was then cancelled as soon
 * as it covered a MainActivity that had authenticated earlier, and the error was dropped, leaving a blank screen.
 */
internal class BiometricAuthenticationController(
    private val activity: FragmentActivity,
    private val onUnclaimedResult: (BiometricAuthenticationResult) -> Unit,
) : DefaultLifecycleObserver {

    private lateinit var prompt: BiometricPrompt
    private var pendingPromptInfo: BiometricPrompt.PromptInfo? = null
    private var resultHandler: ((BiometricAuthenticationResult) -> Unit)? = null
    private var sessionActive = false

    private val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onAuthenticationResult(BiometricAuthenticationResult.Success)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onAuthenticationResult(BiometricAuthenticationResult.Error(errString))
        }
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    fun launch(
        promptInfo: BiometricPrompt.PromptInfo,
        resultHandler: ((BiometricAuthenticationResult) -> Unit)? = null,
    ): Boolean {
        if (sessionActive) return false

        sessionActive = true
        this.resultHandler = resultHandler
        BiometricAuthentication.isAuthenticating = true

        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            prompt.authenticate(promptInfo)
        } else {
            pendingPromptInfo = promptInfo
        }
        return true
    }

    override fun onCreate(owner: LifecycleOwner) {
        // Created with every activity instance so a prompt that survived a configuration change reports here.
        prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
    }

    override fun onStart(owner: LifecycleOwner) {
        pendingPromptInfo?.let(prompt::authenticate)
        pendingPromptInfo = null
    }

    private fun onAuthenticationResult(result: BiometricAuthenticationResult) {
        sessionActive = false
        BiometricAuthentication.isAuthenticating = false

        val handler = resultHandler
        resultHandler = null
        if (handler != null) {
            handler(result)
        } else {
            onUnclaimedResult(result)
        }
    }
}
