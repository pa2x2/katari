package eu.kanade.tachiyomi.ui.reader

import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Applies manga reader visibility and icon-contrast policy to Android system bars.
 *
 * Bars kept visible while reading float over the edge-to-edge page, so their icons follow the content sampled behind
 * them instead of the app theme until the reader menu covers them.
 */
internal class ReaderSystemBars(window: Window) {
    private val controller = WindowCompat.getInsetsController(window, window.decorView)
    private val appUsesDarkStatusBarIcons = controller.isAppearanceLightStatusBars
    private val appUsesDarkNavigationBarIcons = controller.isAppearanceLightNavigationBars
    private val contentSampler = ReaderSystemBarContentSampler(window, onContentChanged = ::applyIconAppearance)

    private var menuVisible = false

    @ColorInt
    private var readerBackgroundColor: Int? = null

    init {
        window.isNavigationBarContrastEnforced = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun apply(
        menuVisible: Boolean,
        keepStatusBarVisible: Boolean,
        keepNavigationBarVisible: Boolean,
        @ColorInt readerBackgroundColor: Int?,
    ) {
        this.menuVisible = menuVisible
        this.readerBackgroundColor = readerBackgroundColor
        if (menuVisible || keepStatusBarVisible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        if (menuVisible || keepNavigationBarVisible) {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
        contentSampler.setEnabled(!menuVisible && (keepStatusBarVisible || keepNavigationBarVisible))
        applyIconAppearance()
    }

    fun release() {
        contentSampler.setEnabled(false)
    }

    private fun applyIconAppearance() {
        if (menuVisible) {
            controller.isAppearanceLightStatusBars = appUsesDarkStatusBarIcons
            controller.isAppearanceLightNavigationBars = appUsesDarkNavigationBarIcons
            return
        }
        val readerBackgroundIsLight = readerBackgroundColor?.let {
            ColorUtils.calculateLuminance(it) > ReaderSystemBarContentSampler.LIGHT_CONTENT_LUMINANCE
        }
        controller.isAppearanceLightStatusBars = contentSampler.statusBarContentIsLight
            ?: readerBackgroundIsLight
            ?: appUsesDarkStatusBarIcons
        controller.isAppearanceLightNavigationBars = contentSampler.navigationBarContentIsLight
            ?: readerBackgroundIsLight
            ?: appUsesDarkNavigationBarIcons
    }
}
