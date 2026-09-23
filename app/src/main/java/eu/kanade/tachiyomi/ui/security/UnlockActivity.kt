package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import logcat.LogPriority
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Blank activity with a BiometricPrompt.
 */
class UnlockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BiometricAuthentication.isAuthenticating) return

        val request = AuthenticationRequest.biometricRequest(
            title = stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
            authFallbacks = arrayOf(AuthenticationRequest.Biometric.Fallback.DeviceCredential),
        ) {
            setIsConfirmationRequired(false)
        }
        launchAuthentication(request)
    }

    override fun onUnclaimedAuthenticationResult(result: AuthenticationResult) {
        when (result) {
            is AuthenticationResult.Success -> {
                val profileManager = Injekt.get<ProfileManager>()
                profileManager.markProfileAuthenticated(profileManager.activeProfileId)
                SecureActivityDelegate.unlock()
                finish()
            }
            is AuthenticationResult.Error -> {
                logcat(LogPriority.ERROR) { result.errString.toString() }
                finishAffinity()
            }
            is AuthenticationResult.CustomFallbackSelected -> {
                logcat(LogPriority.ERROR) { "Unexpected custom fallback selected during app unlock" }
                finishAffinity()
            }
        }
    }
}
