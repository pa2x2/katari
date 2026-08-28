package eu.kanade.tachiyomi.ui.security

import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.isAuthenticationSupported
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.suspendCancellableCoroutine
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import kotlin.coroutines.resume

object BiometricAuthentication {

    /**
     * Avoids a second app-lock request while the system authentication UI temporarily backgrounds
     * Katari on Android versions that host device credential authentication in another activity.
     */
    var isAuthenticating = false
        internal set

    suspend fun FragmentActivity.authenticate(
        title: String,
        subtitle: String? = stringResource(MR.strings.confirm_lock_change),
    ): Boolean {
        if (!isAuthenticationSupported()) return true

        val authenticationActivity = this as? BaseActivity ?: return false
        val request = AuthenticationRequest.biometricRequest(
            title = title,
            authFallbacks = arrayOf(AuthenticationRequest.Biometric.Fallback.DeviceCredential),
        ) {
            setSubtitle(subtitle)
        }

        return suspendCancellableCoroutine { continuation ->
            val launched = authenticationActivity.launchAuthentication(request) { result ->
                if (!continuation.isActive) return@launchAuthentication

                when (result) {
                    is AuthenticationResult.Success -> continuation.resume(true)
                    is AuthenticationResult.Error -> {
                        toast(result.errString.toString())
                        continuation.resume(false)
                    }
                    is AuthenticationResult.CustomFallbackSelected -> continuation.resume(false)
                }
            }
            if (!launched) continuation.resume(false)
        }
    }
}
