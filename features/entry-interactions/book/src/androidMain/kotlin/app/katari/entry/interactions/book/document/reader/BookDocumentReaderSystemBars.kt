package app.katari.entry.interactions.book.document.reader

import android.os.Build
import android.view.Window
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.katari.entry.interactions.reader.settings.BookDocumentReaderTheme

/** Applies BOOK document reader visibility and icon-contrast policy to Android system bars. */
internal class BookDocumentReaderSystemBars(window: Window) {
    private val controller = WindowCompat.getInsetsController(window, window.decorView)
    private val appUsesDarkStatusBarIcons = controller.isAppearanceLightStatusBars
    private val appUsesDarkNavigationBarIcons = controller.isAppearanceLightNavigationBars

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /** A null [request] gives the app's own bars back, as while the reader shows no book. */
    fun apply(request: BookDocumentReaderSystemBarsRequest?) {
        if (request == null) return showAppBars()
        if (request.chromeVisible || request.keepStatusBarVisible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        if (request.chromeVisible || request.keepNavigationBarVisible) {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
        applySystemBarIconAppearance(request.readerTheme)
    }

    private fun applySystemBarIconAppearance(
        readerTheme: BookDocumentReaderTheme,
    ) {
        val readerHasDarkBackground = bookDocumentReaderThemeHasDarkBackground(readerTheme)
        controller.isAppearanceLightStatusBars = readerHasDarkBackground?.not() ?: appUsesDarkStatusBarIcons
        controller.isAppearanceLightNavigationBars =
            readerHasDarkBackground?.not() ?: appUsesDarkNavigationBarIcons
    }

    private fun showAppBars() {
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = appUsesDarkStatusBarIcons
        controller.isAppearanceLightNavigationBars = appUsesDarkNavigationBarIcons
    }
}

/** Null for the app theme, whose own light or dark look decides. */
private fun bookDocumentReaderThemeHasDarkBackground(theme: BookDocumentReaderTheme): Boolean? = when (theme) {
    BookDocumentReaderTheme.App -> null
    BookDocumentReaderTheme.Paper -> false
    BookDocumentReaderTheme.Dusk,
    BookDocumentReaderTheme.Black,
    -> true
    is BookDocumentReaderTheme.Custom -> ColorUtils.calculateLuminance(theme.background) < DARK_PAGE_LUMINANCE
}

private const val DARK_PAGE_LUMINANCE = 0.5
