package tachiyomi.presentation.core.components.reader

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ReaderSystemBarsEffect(enabled: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(context, view, enabled) {
        val activity = context as? Activity
        if (activity == null || !enabled) {
            onDispose {}
        } else {
            val controller = WindowInsetsControllerCompat(activity.window, view)
            val previousBehavior = controller.systemBarsBehavior
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                controller.systemBarsBehavior = previousBehavior
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
