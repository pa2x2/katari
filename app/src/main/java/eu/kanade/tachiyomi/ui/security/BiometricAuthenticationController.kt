package eu.kanade.tachiyomi.ui.security

import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/** Owns the single Activity Result authentication launcher required by a Katari activity. */
internal class BiometricAuthenticationController(
    private val activity: FragmentActivity,
    private val onUnclaimedResult: (AuthenticationResult) -> Unit,
) : DefaultLifecycleObserver {

    private val launcher = activity.registerForAuthenticationResult(::onAuthenticationResult)
    private var pendingRequest: AuthenticationRequest? = null
    private var resultHandler: ((AuthenticationResult) -> Unit)? = null
    private var sessionActive = false

    init {
        activity.lifecycle.addObserver(this)
    }

    fun launch(
        request: AuthenticationRequest,
        resultHandler: ((AuthenticationResult) -> Unit)? = null,
    ): Boolean {
        if (sessionActive) return false

        sessionActive = true
        this.resultHandler = resultHandler
        BiometricAuthentication.isAuthenticating = true

        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            launcher.launch(request)
        } else {
            pendingRequest = request
        }
        return true
    }

    override fun onStart(owner: LifecycleOwner) {
        pendingRequest?.let(launcher::launch)
        pendingRequest = null
    }

    private fun onAuthenticationResult(result: AuthenticationResult) {
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
