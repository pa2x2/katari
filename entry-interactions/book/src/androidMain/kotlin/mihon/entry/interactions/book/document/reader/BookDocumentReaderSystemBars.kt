package mihon.entry.interactions.book.document.reader

import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderThemeHasDarkBackground
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme

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

    fun apply(
        chromeVisible: Boolean,
        keepStatusBarVisible: Boolean,
        keepNavigationBarVisible: Boolean,
        readerTheme: BookDocumentReaderTheme,
    ) {
        if (chromeVisible || keepStatusBarVisible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        if (chromeVisible || keepNavigationBarVisible) {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
        applySystemBarIconAppearance(readerTheme)
    }

    private fun applySystemBarIconAppearance(
        readerTheme: BookDocumentReaderTheme,
    ) {
        val readerHasDarkBackground = bookDocumentReaderThemeHasDarkBackground(readerTheme)
        controller.isAppearanceLightStatusBars = readerHasDarkBackground?.not() ?: appUsesDarkStatusBarIcons
        controller.isAppearanceLightNavigationBars =
            readerHasDarkBackground?.not() ?: appUsesDarkNavigationBarIcons
    }

    fun showAppBars() {
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = appUsesDarkStatusBarIcons
        controller.isAppearanceLightNavigationBars = appUsesDarkNavigationBarIcons
    }
}
