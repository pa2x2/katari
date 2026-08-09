package mihon.entry.interactions.book.document.reader

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode

/** Applies BOOK document reader visibility and icon-contrast policy to Android system bars. */
internal class BookDocumentReaderSystemBars(window: Window) {
    private val controller = WindowCompat.getInsetsController(window, window.decorView)
    private val appUsesDarkStatusBarIcons = controller.isAppearanceLightStatusBars

    init {
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun apply(
        chromeVisible: Boolean,
        keepStatusBarVisible: Boolean,
        readerTheme: BookDocumentReaderThemeMode,
    ) {
        if (chromeVisible || keepStatusBarVisible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
        applyStatusBarIconAppearance(chromeVisible, readerTheme)
    }

    private fun applyStatusBarIconAppearance(
        chromeVisible: Boolean,
        readerTheme: BookDocumentReaderThemeMode,
    ) {
        controller.isAppearanceLightStatusBars = when {
            chromeVisible -> appUsesDarkStatusBarIcons
            readerTheme == BookDocumentReaderThemeMode.BLACK -> false
            else -> appUsesDarkStatusBarIcons
        }
    }

    fun showAppBars() {
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = appUsesDarkStatusBarIcons
    }
}
