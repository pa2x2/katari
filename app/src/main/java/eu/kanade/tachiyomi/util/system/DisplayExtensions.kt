package eu.kanade.tachiyomi.util.system

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View

/**
 * Returns true if current context is in night mode
 */
fun Context.isNightMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Checks whether if the device has a display cutout (i.e. notch, camera cutout, etc.).
 *
 * Only works on Android 9+.
 */
fun Activity.hasDisplayCutout(): Boolean {
    return window.decorView.hasDisplayCutout()
}

/**
 * Checks whether if the device has a display cutout (i.e. notch, camera cutout, etc.).
 *
 * Only works on Android 9+.
 */
fun View.hasDisplayCutout(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && rootWindowInsets?.displayCutout != null
}

/**
 * Gets system's config_navBarNeedsScrim boolean flag added in Android 10, defaults to true.
 */
fun Context.isNavigationBarNeedsScrim(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        InternalResourceHelper.getBoolean(this, "config_navBarNeedsScrim", true)
}
