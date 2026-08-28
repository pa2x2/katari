package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegateImpl
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegate
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import eu.kanade.tachiyomi.ui.security.BiometricAuthenticationController
import eu.kanade.tachiyomi.util.system.prepareTabletUiContext

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    private val biometricAuthenticationController = BiometricAuthenticationController(
        activity = this,
        onUnclaimedResult = ::onUnclaimedAuthenticationResult,
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.prepareTabletUiContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }

    internal fun launchAuthentication(
        request: AuthenticationRequest,
        resultHandler: ((AuthenticationResult) -> Unit)? = null,
    ): Boolean = biometricAuthenticationController.launch(request, resultHandler)

    protected open fun onUnclaimedAuthenticationResult(result: AuthenticationResult) = Unit
}
