package app.katari.presentation.kit.window

import android.os.Build
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Android doesn't turn a window in split screen, nor, from Android 16, on a screen at least 600 dp both ways such as a
 * tablet or an unfolded foldable. The window's shorter side stands in for the screen's, as they match whenever the
 * window isn't split.
 */
internal actual fun honoursOrientationRequests(size: DpSize, inMultiWindow: Boolean): Boolean {
    val ignoresOrientationOnLargeScreens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    return !inMultiWindow &&
        !(ignoresOrientationOnLargeScreens && minOf(size.width, size.height) >= LargeScreenShortSideMin)
}

private val LargeScreenShortSideMin = 600.dp
