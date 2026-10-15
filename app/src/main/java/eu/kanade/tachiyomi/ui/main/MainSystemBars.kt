package eu.kanade.tachiyomi.ui.main

import android.graphics.Color
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Draws the main screen edge to edge behind see-through system bars. [lightStatusBar] says the fill behind the status
 * bar is light, so its icons turn dark; the navigation bar's icons follow [darkTheme].
 */
internal fun ComponentActivity.applyMainSystemBarStyle(lightStatusBar: Boolean, darkTheme: Boolean) {
    val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
    val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    enableEdgeToEdge(
        statusBarStyle = if (lightStatusBar) lightStyle else darkStyle,
        navigationBarStyle = if (darkTheme) darkStyle else lightStyle,
    )
}

/** Clears the bar colours for the pre-Android 12 splash exit animation, which restores them when it finishes. */
@Suppress("Deprecation")
internal fun Window.clearSystemBarColors() {
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
}
