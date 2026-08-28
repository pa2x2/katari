package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

/** Returns whether a Class 2 biometric or device credential is available for authentication. */
fun Context.isAuthenticationSupported(): Boolean {
    val biometricManager = BiometricManager.from(this)
    return biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL) ==
        BiometricManager.BIOMETRIC_SUCCESS ||
        biometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS ||
        biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
}
