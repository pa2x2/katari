package tachiyomi.presentation.core.util

import android.view.ViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun scrollbarThickness(): Float {
    val context = LocalContext.current
    return remember { ViewConfiguration.get(context).scaledScrollBarSize.toFloat() }
}

internal actual val ScrollbarFadeDelayMillis: Long = ViewConfiguration.getScrollDefaultDelay().toLong()
internal actual val ScrollbarFadeDurationMillis: Int = ViewConfiguration.getScrollBarFadeDuration()
