package eu.kanade.tachiyomi.ui.security

import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.isAuthenticationSupported
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.suspendCancellableCoroutine
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import kotlin.coroutines.resume

object BiometricAuthentication {

    /**
     * Avoids a second app-lock request while the system authentication UI temporarily backgrounds
     * Katari on Android versions that host device credential authentication in another activity.
     */
    var isAuthenticating = false
        internal set

    internal fun promptInfo(
        title: String,
        subtitle: String? = null,
        confirmationRequired: Boolean = true,
    ): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setConfirmationRequired(confirmationRequired)
        .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
        .build()

    suspend fun FragmentActivity.authenticate(
        title: String,
        subtitle: String? = stringResource(MR.strings.confirm_lock_change),
    ): Boolean {
        if (!isAuthenticationSupported()) return true

        val authenticationActivity = this as? BaseActivity ?: return false
        return suspendCancellableCoroutine { continuation ->
            val launched = authenticationActivity.launchAuthentication(promptInfo(title, subtitle)) { result ->
                if (!continuation.isActive) return@launchAuthentication

                when (result) {
                    BiometricAuthenticationResult.Success -> continuation.resume(true)
                    is BiometricAuthenticationResult.Error -> {
                        toast(result.message.toString())
                        continuation.resume(false)
                    }
                }
            }
            if (!launched) continuation.resume(false)
        }
    }
}
