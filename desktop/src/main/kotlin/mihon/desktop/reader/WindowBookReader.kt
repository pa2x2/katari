package mihon.desktop.reader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.core.platform.PlatformContext
import mihon.desktop.window.WindowPlatformContext
import mihon.entry.interactions.reader.window.BookReader
import mihon.entry.interactions.reader.window.BookReaderDestination
import mihon.entry.interactions.reader.window.BookReaderWindowRequest

/**
 * The book reader in the app's window: a screen of the window's navigation that the reader fills
 * ([BookReaderScreen]). A chapter opened while the reader is on top, as from a notification, takes its place.
 */
@SingleIn(AppScope::class)
@Inject
class WindowBookReader(
    internal val reader: BookReader,
    internal val webViewDestination: WebViewDestination,
) : BookReaderDestination {
    override fun open(context: PlatformContext, request: BookReaderWindowRequest) {
        val navigator = (context as? WindowPlatformContext)?.navigator
            ?: error("The reader opens only from a window that shows the app's screens")
        val screen = BookReaderScreen(request, this)
        if (navigator.lastItem is BookReaderScreen) navigator.replace(screen) else navigator.push(screen)
    }
}
